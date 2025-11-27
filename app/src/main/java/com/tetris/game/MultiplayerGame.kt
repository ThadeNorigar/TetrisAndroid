package com.tetris.game

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tetris.network.GameMessage
import com.tetris.network.NetworkManager
import com.tetris.ui.theme.TetrisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for multiplayer game
 * Manages two game instances and network communication
 */
class MultiplayerGameViewModel(
    application: Application,
    private val networkManager: NetworkManager,
    private val theme: TetrisTheme
) : AndroidViewModel(application) {

    private val tag = "MultiplayerGame"

    // Local player's game
    val localGame = TetrisGame(theme.shapeColors)

    // Opponent's game state (read-only, updated via network)
    private val _opponentBoardState = MutableStateFlow<List<List<Color?>>>(emptyList())
    val opponentBoardState: StateFlow<List<List<Color?>>> = _opponentBoardState.asStateFlow()

    private val _opponentStats = MutableStateFlow(GameStats())
    val opponentStats: StateFlow<GameStats> = _opponentStats.asStateFlow()

    private val _opponentCurrentPiece = MutableStateFlow<Tetromino?>(null)
    val opponentCurrentPiece: StateFlow<Tetromino?> = _opponentCurrentPiece.asStateFlow()

    private val _opponentNextPiece = MutableStateFlow<Tetromino?>(null)
    val opponentNextPiece: StateFlow<Tetromino?> = _opponentNextPiece.asStateFlow()

    // Winner state
    private val _winner = MutableStateFlow<Winner?>(null)
    val winner: StateFlow<Winner?> = _winner.asStateFlow()

    // Connection state from network manager
    val connectionState: StateFlow<com.tetris.network.ConnectionState> = networkManager.connectionState

    // Track lines cleared to send as garbage
    private var lastLinesCleared = 0

    // Track if we were ever connected (to distinguish initial state from disconnection)
    private var wasConnected = false

    init {
        startGame()
        observeNetworkMessages()
        observeLocalGameState()
        observeConnectionState()
    }

    private fun startGame() {
        // Reset all state for a fresh game
        _opponentBoardState.value = emptyList()
        _opponentStats.value = GameStats()
        _opponentCurrentPiece.value = null
        _opponentNextPiece.value = null
        _winner.value = null
        lastLinesCleared = 0

        localGame.startGame()
    }

    private fun observeLocalGameState() {
        // Monitor local game stats to detect line clears
        viewModelScope.launch {
            localGame.stats.collect { stats ->
                val newLinesCleared = stats.linesCleared - lastLinesCleared
                if (newLinesCleared > 0) {
                    // Send garbage lines to opponent
                    sendGarbageLines(newLinesCleared)
                    lastLinesCleared = stats.linesCleared
                }
            }
        }

        // Monitor local game state for game over
        viewModelScope.launch {
            localGame.gameState.collect { state ->
                if (state is GameState.GameOver) {
                    // Local player lost
                    _winner.value = Winner.Opponent
                    networkManager.sendMessage(
                        GameMessage.GameOver(
                            score = state.score,
                            level = state.level,
                            linesCleared = state.lines
                        )
                    )
                }
            }
        }

        // Send board updates periodically (every 500ms for better stability)
        viewModelScope.launch {
            var lastBoardState: List<List<Color?>> = emptyList()
            while (true) {
                delay(500)  // Reduced from 100ms to 500ms to avoid flooding
                if (localGame.gameState.value is GameState.Playing) {
                    // Only send if board actually changed
                    val currentBoard = localGame.boardState.value
                    if (currentBoard != lastBoardState) {
                        try {
                            sendBoardUpdate()
                            lastBoardState = currentBoard
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to send board update", e)
                        }
                    }
                }
            }
        }

        // Send stats updates periodically (debounced)
        viewModelScope.launch {
            localGame.stats
                .debounce(300)  // Debounce to avoid flooding
                .collect { stats ->
                    try {
                        networkManager.sendMessage(
                            GameMessage.StatsUpdate(
                                score = stats.score,
                                level = stats.level,
                                linesCleared = stats.linesCleared
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to send stats update", e)
                    }
                }
        }

        // Send current piece updates (debounced to avoid flooding)
        viewModelScope.launch {
            localGame.currentPiece
                .debounce(50)  // Debounce to 50ms for smooth but not excessive updates
                .collect { piece ->
                    if (piece != null) {
                        try {
                            networkManager.sendMessage(
                                GameMessage.CurrentPieceUpdate(
                                    pieceType = piece.type.name,
                                    shape = piece.shape,
                                    colorInt = piece.color.toArgb(),
                                    x = piece.x,
                                    y = piece.y
                                )
                            )
                            Log.d(tag, "Sent current piece: ${piece.type.name} at (${piece.x}, ${piece.y})")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to send current piece update", e)
                        }
                    }
                }
        }

        // Send next piece updates (debounced)
        viewModelScope.launch {
            localGame.nextPiece
                .debounce(100)  // Next piece changes less frequently
                .collect { piece ->
                    if (piece != null) {
                        try {
                            networkManager.sendMessage(
                                GameMessage.NextPieceUpdate(
                                    pieceType = piece.type.name,
                                    colorInt = piece.color.toArgb()
                                )
                            )
                            Log.d(tag, "Sent next piece: ${piece.type.name}")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to send next piece update", e)
                        }
                    }
                }
        }
    }

    private fun observeNetworkMessages() {
        viewModelScope.launch {
            networkManager.receivedMessages.collect { message ->
                message?.let { handleNetworkMessage(it) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is com.tetris.network.ConnectionState.Reconnecting -> {
                        // Pause game during reconnection
                        localGame.pauseGame()
                        Log.d(tag, "Connection lost, reconnecting... (attempt ${state.attempt})")
                    }
                    is com.tetris.network.ConnectionState.Connected -> {
                        // Mark that we successfully connected
                        wasConnected = true
                        // Resume game after reconnection
                        if (localGame.gameState.value is GameState.Paused) {
                            localGame.resumeGame()
                            Log.d(tag, "Connection restored, resuming game")
                        }
                    }
                    is com.tetris.network.ConnectionState.Disconnected -> {
                        // Only treat as disconnection if we were previously connected
                        // This prevents treating the initial state as a disconnection
                        if (wasConnected && _winner.value == null) {
                            // Connection permanently lost after being connected
                            _winner.value = Winner.Disconnected
                            Log.d(tag, "Connection lost - game ended")
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleNetworkMessage(message: GameMessage) {
        when (message) {
            is GameMessage.StatsUpdate -> {
                _opponentStats.value = GameStats(
                    score = message.score,
                    level = message.level,
                    linesCleared = message.linesCleared
                )
            }

            is GameMessage.BoardUpdate -> {
                // Convert Int colors back to Color objects
                val boardWithColors = message.board.map { row ->
                    row.map { colorInt ->
                        if (colorInt != null) {
                            androidx.compose.ui.graphics.Color(colorInt)
                        } else {
                            null
                        }
                    }
                }
                _opponentBoardState.value = boardWithColors
            }

            is GameMessage.CurrentPieceUpdate -> {
                // Update opponent's current piece
                try {
                    val type = TetrominoType.valueOf(message.pieceType)
                    val color = Color(message.colorInt)
                    val piece = Tetromino(
                        type = type,
                        shape = message.shape,
                        color = color,
                        x = message.x,
                        y = message.y
                    )
                    _opponentCurrentPiece.value = piece
                    Log.d(tag, "Received opponent current piece: ${type.name} at (${message.x}, ${message.y})")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse current piece update", e)
                }
            }

            is GameMessage.NextPieceUpdate -> {
                // Update opponent's next piece
                try {
                    val type = TetrominoType.valueOf(message.pieceType)
                    val color = Color(message.colorInt)
                    val piece = Tetromino.create(type, color)
                    _opponentNextPiece.value = piece
                    Log.d(tag, "Received opponent next piece: ${type.name}")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse next piece update", e)
                }
            }

            is GameMessage.PieceUpdate -> {
                // Legacy message - now using CurrentPieceUpdate instead
                Log.d(tag, "Opponent piece update: (${message.x}, ${message.y})")
            }

            is GameMessage.GarbageReceived -> {
                // Receive garbage lines from opponent
                receiveGarbageLines(message.count)
            }

            is GameMessage.GameOver -> {
                // Opponent lost, local player wins!
                _winner.value = Winner.LocalPlayer
            }

            is GameMessage.PlayerDisconnected -> {
                // Opponent disconnected
                _winner.value = Winner.Disconnected
            }

            else -> {
                Log.d(tag, "Unhandled message: $message")
            }
        }
    }

    private fun sendBoardUpdate() {
        viewModelScope.launch {
            // Get current board state and convert Colors to Int (ARGB)
            val boardState = localGame.boardState.value
            val boardAsInts: List<List<Int?>> = boardState.map { row: List<Color?> ->
                row.map { color: Color? ->
                    color?.toArgb()
                }
            }

            networkManager.sendMessage(
                GameMessage.BoardUpdate(board = boardAsInts)
            )
        }
    }

    private fun sendGarbageLines(count: Int) {
        viewModelScope.launch {
            // In Tetris, clearing lines sends garbage to opponent
            // 1 line = 0 garbage, 2 lines = 1 garbage, 3 lines = 2 garbage, 4 lines = 4 garbage
            val garbageCount = when (count) {
                1 -> 0
                2 -> 1
                3 -> 2
                4 -> 4
                else -> count - 1
            }

            if (garbageCount > 0) {
                networkManager.sendMessage(GameMessage.GarbageReceived(garbageCount))
                Log.d(tag, "Sent $garbageCount garbage lines to opponent")
            }
        }
    }

    private fun receiveGarbageLines(count: Int) {
        viewModelScope.launch {
            // Add garbage lines to local player's board
            val success = addGarbageToLocalGame(count)
            if (!success) {
                // Adding garbage caused game over
                val stats = localGame.stats.value
                _winner.value = Winner.Opponent
                networkManager.sendMessage(
                    GameMessage.GameOver(
                        score = stats.score,
                        level = stats.level,
                        linesCleared = stats.linesCleared
                    )
                )
            }
            Log.d(tag, "Received $count garbage lines from opponent")
        }
    }

    private fun addGarbageToLocalGame(count: Int): Boolean {
        return localGame.addGarbageLines(count)
    }

    // Game controls - delegate to local game
    fun moveLeft() = localGame.moveLeft()
    fun moveRight() = localGame.moveRight()
    fun moveDown() = localGame.moveDown()
    fun rotatePiece() = localGame.rotatePiece()
    fun hardDrop() = localGame.hardDrop()
    fun pauseGame() = localGame.pauseGame()
    fun resumeGame() = localGame.resumeGame()

    /**
     * Cleanup resources when leaving the game
     * Can be called manually when returning to menu
     */
    fun cleanup() {
        Log.d(tag, "=== cleanup() CALLED ===")
        Log.d(tag, "Stack trace: ${Exception().stackTraceToString()}")
        localGame.dispose()
        networkManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "=== onCleared() CALLED ===")
        cleanup()
    }
}

/**
 * Winner of the multiplayer game
 */
sealed class Winner {
    object LocalPlayer : Winner()      // Local player won by opponent losing
    object Opponent : Winner()          // Opponent won by local player losing
    object Disconnected : Winner()      // Connection lost
}
