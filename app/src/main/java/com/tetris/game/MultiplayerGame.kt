package com.tetris.game

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tetris.network.GameMessage
import com.tetris.network.NetworkManager
import com.tetris.ui.theme.TetrisTheme
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

        // Send board updates periodically (every 100ms for smooth display)
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (localGame.gameState.value is GameState.Playing) {
                    sendBoardUpdate()
                }
            }
        }

        // Send stats updates periodically
        viewModelScope.launch {
            localGame.stats.collect { stats ->
                networkManager.sendMessage(
                    GameMessage.StatsUpdate(
                        score = stats.score,
                        level = stats.level,
                        linesCleared = stats.linesCleared
                    )
                )
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

            is GameMessage.PieceUpdate -> {
                // Update opponent's current piece position
                // Note: This is simplified - in a full implementation,
                // you'd need to track piece type and rotation
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
            val boardAsInts = boardState.map { row ->
                row.map { color ->
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
