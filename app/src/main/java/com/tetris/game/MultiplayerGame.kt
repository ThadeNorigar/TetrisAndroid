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

    // Create empty board helper
    private fun createEmptyBoard(): List<List<Color?>> {
        return List(20) { List(10) { null } }  // 20 rows x 10 columns
    }

    // Opponent's game state (read-only, updated via network)
    private val _opponentBoardState = MutableStateFlow<List<List<Color?>>>(createEmptyBoard())
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

    // Play again ready states
    private val _localPlayerReady = MutableStateFlow(false)
    val localPlayerReady: StateFlow<Boolean> = _localPlayerReady.asStateFlow()

    private val _opponentReady = MutableStateFlow(false)
    val opponentReady: StateFlow<Boolean> = _opponentReady.asStateFlow()

    // Track if opponent left the game
    private val _opponentLeft = MutableStateFlow(false)
    val opponentLeft: StateFlow<Boolean> = _opponentLeft.asStateFlow()

    // Connection state from network manager
    val connectionState: StateFlow<com.tetris.network.ConnectionState> = networkManager.connectionState

    // Track lines cleared to send as garbage
    private var lastLinesCleared = 0

    // Track if we were ever connected (to distinguish initial state from disconnection)
    private var wasConnected = false

    // Track if cleanup was already called
    private var cleanedUp = false

    // Callback for when cleanup is complete
    private var onCleanupComplete: (() -> Unit)? = null

    // Jobs for observation coroutines that need to be cancelled on cleanup
    private var observeNetworkJob: kotlinx.coroutines.Job? = null
    private var observeConnectionJob: kotlinx.coroutines.Job? = null
    private val observeLocalGameJobs = mutableListOf<kotlinx.coroutines.Job>()

    // Separate list for sending jobs that should stop on GameOver
    private val sendingJobs = mutableListOf<kotlinx.coroutines.Job>()

    init {
        observeNetworkMessages()
        observeLocalGameState()
        observeConnectionState()
        startGame() // This will also call startSendingUpdates()
    }

    private fun startGame() {
        // Reset all state for a fresh game
        _opponentBoardState.value = createEmptyBoard()  // Proper empty board, not emptyList()
        _opponentStats.value = GameStats()
        _opponentCurrentPiece.value = null
        _opponentNextPiece.value = null
        _winner.value = null
        _localPlayerReady.value = false
        _opponentReady.value = false
        _opponentLeft.value = false
        lastLinesCleared = 0

        localGame.startGame()

        // Restart sending updates for new game
        startSendingUpdates()

        Log.d(tag, "Game started - all state reset with empty 20x10 board, sending restarted")
    }

    private fun observeLocalGameState() {
        // Monitor local game stats to detect line clears
        observeLocalGameJobs.add(viewModelScope.launch {
            localGame.stats.collect { stats ->
                val newLinesCleared = stats.linesCleared - lastLinesCleared
                if (newLinesCleared > 0) {
                    // Send garbage lines to opponent
                    sendGarbageLines(newLinesCleared)
                    lastLinesCleared = stats.linesCleared
                }
            }
        })

        // Monitor local game state for game over
        observeLocalGameJobs.add(viewModelScope.launch {
            localGame.gameState.collect { state ->
                if (state is GameState.GameOver) {
                    // Local player lost
                    _winner.value = Winner.Opponent
                    localGame.pauseGame() // Stop the game immediately
                    stopSendingUpdates() // Stop sending network updates
                    networkManager.sendMessage(
                        GameMessage.GameOver(
                            score = state.score,
                            level = state.level,
                            linesCleared = state.lines
                        )
                    )
                    Log.d(tag, "Local player lost - game stopped, updates stopped")
                }
            }
        })
    }

    /**
     * Start sending game updates over network
     * Creates coroutines to send board, stats, and piece updates
     */
    private fun startSendingUpdates() {
        // Cancel any existing sending jobs first
        sendingJobs.forEach { it.cancel() }
        sendingJobs.clear()
        Log.d(tag, "Starting sending updates...")

        // Send initial state immediately to ensure synchronization
        viewModelScope.launch {
            // Send initial board + current piece
            try {
                sendBoardUpdate()
                Log.d(tag, "Sent initial board update")
            } catch (e: Exception) {
                Log.e(tag, "Failed to send initial board update", e)
            }

            // Send initial next piece
            localGame.nextPiece.value?.let { piece ->
                try {
                    networkManager.sendMessage(
                        GameMessage.NextPieceUpdate(
                            pieceType = piece.type.name,
                            colorInt = piece.color.toArgb()
                        )
                    )
                    Log.d(tag, "Sent initial next piece: ${piece.type.name}")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to send initial next piece", e)
                }
            }

            // Send initial stats
            try {
                val stats = localGame.stats.value
                networkManager.sendMessage(
                    GameMessage.StatsUpdate(
                        score = stats.score,
                        level = stats.level,
                        linesCleared = stats.linesCleared
                    )
                )
                Log.d(tag, "Sent initial stats update")
            } catch (e: Exception) {
                Log.e(tag, "Failed to send initial stats", e)
            }
        }

        // Send board updates periodically - now includes current piece for atomic sync
        sendingJobs.add(viewModelScope.launch {
            while (true) {
                delay(30)  // 30ms = 33fps for smooth atomic board+piece sync
                if (localGame.gameState.value is GameState.Playing) {
                    // Send board update with current piece for atomic synchronization
                    // This ensures opponent always has consistent board+piece state
                    try {
                        sendBoardUpdate()
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to send board update", e)
                    }
                }
            }
        })

        // Send stats updates periodically (debounced)
        sendingJobs.add(viewModelScope.launch {
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
        })

        // Monitor for piece locking to send immediate update
        sendingJobs.add(viewModelScope.launch {
            localGame.currentPiece
                .collect { piece ->
                    if (piece == null && localGame.gameState.value is GameState.Playing) {
                        // Piece became null (locked) - send immediate board update
                        try {
                            sendBoardUpdate()
                            Log.d(tag, "Piece locked - sent immediate board update")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to send board update on piece lock", e)
                        }
                    }
                }
        })

        // Send next piece updates (debounced)
        sendingJobs.add(viewModelScope.launch {
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
        })

        Log.d(tag, "All sending jobs started")
    }

    private fun observeNetworkMessages() {
        observeNetworkJob = viewModelScope.launch {
            networkManager.receivedMessages.collect { message ->
                message?.let { handleNetworkMessage(it) }
            }
        }
    }

    private fun observeConnectionState() {
        observeConnectionJob = viewModelScope.launch {
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

                // Update current piece from board update for atomic synchronization
                // This prevents visual gaps when pieces lock
                if (message.currentPieceType != null && message.currentPieceShape != null &&
                    message.currentPieceColor != null && message.currentPieceX != null &&
                    message.currentPieceY != null) {
                    try {
                        val type = TetrominoType.valueOf(message.currentPieceType)
                        val color = Color(message.currentPieceColor)
                        val piece = Tetromino(
                            type = type,
                            shape = message.currentPieceShape,
                            color = color,
                            x = message.currentPieceX,
                            y = message.currentPieceY
                        )
                        _opponentCurrentPiece.value = piece
                        Log.d(tag, "Updated opponent piece from BoardUpdate: ${type.name} at (${message.currentPieceX}, ${message.currentPieceY})")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to parse current piece from BoardUpdate", e)
                    }
                } else {
                    // No current piece in update - piece is locked or null
                    _opponentCurrentPiece.value = null
                    Log.d(tag, "Cleared opponent piece from BoardUpdate (piece locked)")
                }
            }

            is GameMessage.CurrentPieceUpdate -> {
                // DEPRECATED: CurrentPieceUpdate is now handled by BoardUpdate for atomic sync
                // Keeping handler for backward compatibility but piece state comes from BoardUpdate
                Log.d(tag, "Received deprecated CurrentPieceUpdate - ignoring (using BoardUpdate instead)")
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
                localGame.pauseGame() // Stop the winner's game immediately
                stopSendingUpdates() // Stop sending network updates
                Log.d(tag, "Opponent lost - local player wins, game stopped, updates stopped")
            }

            is GameMessage.PlayAgainRequest -> {
                // Opponent wants to play again
                _opponentReady.value = true
                Log.d(tag, "Opponent ready to play again")
                // Check if both players are ready
                if (_localPlayerReady.value && _opponentReady.value) {
                    Log.d(tag, "Both players ready - starting new game")
                    startGame()
                }
            }

            is GameMessage.PlayerLeftGame -> {
                // Opponent left to menu
                _opponentLeft.value = true
                _opponentReady.value = false
                Log.d(tag, "Opponent left the game")
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

    /**
     * Check if a tetromino would collide with occupied cells in the board
     * Used to detect when opponent's piece has been locked
     */
    private fun wouldCollideWithBoard(piece: Tetromino, board: List<List<Color?>>): Boolean {
        for (row in piece.shape.indices) {
            for (col in piece.shape[row].indices) {
                if (piece.shape[row][col] != 0) {
                    val boardRow = piece.y + row
                    val boardCol = piece.x + col

                    // Check if position is within board bounds
                    if (boardRow in board.indices && boardCol in board[boardRow].indices) {
                        // Check if this position on the board is occupied
                        if (board[boardRow][boardCol] != null) {
                            return true  // Collision detected
                        }
                    }
                }
            }
        }
        return false  // No collision
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

            // Include current piece in board update for atomic synchronization
            val currentPiece = localGame.currentPiece.value

            networkManager.sendMessage(
                GameMessage.BoardUpdate(
                    board = boardAsInts,
                    currentPieceType = currentPiece?.type?.name,
                    currentPieceShape = currentPiece?.shape,
                    currentPieceColor = currentPiece?.color?.toArgb(),
                    currentPieceX = currentPiece?.x,
                    currentPieceY = currentPiece?.y
                )
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
     * Request to play again - waits for both players to be ready
     */
    fun requestPlayAgain() {
        Log.d(tag, "Local player wants to play again")
        _localPlayerReady.value = true

        // Send play again request to opponent
        viewModelScope.launch {
            networkManager.sendMessage(GameMessage.PlayAgainRequest)
        }

        // Check if both players are ready
        if (_localPlayerReady.value && _opponentReady.value) {
            Log.d(tag, "Both players ready - starting new game")
            startGame()
        }
    }

    /**
     * Leave the game and return to menu
     * Notifies opponent before disconnecting
     * Calls onComplete callback after cleanup is finished
     */
    fun leaveGame(onComplete: () -> Unit) {
        Log.d(tag, "=== leaveGame() CALLED ===")
        // Notify opponent that we're leaving
        viewModelScope.launch {
            try {
                networkManager.sendMessage(GameMessage.PlayerLeftGame)
                delay(200) // Give more time for message to send
                Log.d(tag, "PlayerLeftGame message sent, now cleaning up")
            } catch (e: Exception) {
                Log.e(tag, "Failed to send PlayerLeftGame message", e)
            } finally {
                // Always cleanup, even if send fails
                cleanup()
                // Call completion callback after cleanup finishes
                Log.d(tag, "Cleanup complete, calling onComplete callback")
                onComplete()
            }
        }
    }

    /**
     * Stop sending game updates over network
     * Called when game ends to prevent sending updates after GameOver
     */
    private fun stopSendingUpdates() {
        Log.d(tag, "Stopping sending updates...")
        sendingJobs.forEach { it.cancel() }
        sendingJobs.clear()
        Log.d(tag, "All sending jobs cancelled")
    }

    /**
     * Cleanup resources when leaving the game
     * Can be called manually when returning to menu
     */
    fun cleanup() {
        if (cleanedUp) {
            Log.d(tag, "cleanup() already called, skipping")
            return
        }
        cleanedUp = true

        Log.d(tag, "=== cleanup() CALLED ===")
        Log.d(tag, "Stack trace: ${Exception().stackTraceToString()}")

        // Cancel all observation coroutines
        Log.d(tag, "Cancelling observation jobs...")
        observeNetworkJob?.cancel()
        observeConnectionJob?.cancel()
        observeLocalGameJobs.forEach { it.cancel() }
        observeLocalGameJobs.clear()
        sendingJobs.forEach { it.cancel() }
        sendingJobs.clear()
        Log.d(tag, "All observation jobs cancelled")

        localGame.dispose()
        networkManager.disconnect()
        Log.d(tag, "=== cleanup() COMPLETED ===")
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
