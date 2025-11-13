package com.tetris.game

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main Tetris game engine
 */
class TetrisGame(
    private val colorScheme: Map<TetrominoType, Color>
) {
    private val board = Board()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _gameState = MutableStateFlow<GameState>(GameState.Menu)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats.asStateFlow()

    private val _currentPiece = MutableStateFlow<Tetromino?>(null)
    val currentPiece: StateFlow<Tetromino?> = _currentPiece.asStateFlow()

    private val _nextPiece = MutableStateFlow<Tetromino?>(null)
    val nextPiece: StateFlow<Tetromino?> = _nextPiece.asStateFlow()

    private val _boardState = MutableStateFlow<List<List<Color?>>>(emptyList())
    val boardState: StateFlow<List<List<Color?>>> = _boardState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lastDropTime = 0L

    /**
     * Start a new game
     */
    fun startGame() {
        board.reset()
        _stats.value = GameStats()
        _currentPiece.value = null
        _nextPiece.value = spawnPiece()
        _gameState.value = GameState.Playing

        spawnNextPiece()
        startGameLoop()
    }

    /**
     * Pause the game
     */
    fun pauseGame() {
        if (_gameState.value is GameState.Playing) {
            _gameState.value = GameState.Paused
            gameLoopJob?.cancel()
        }
    }

    /**
     * Resume the game
     */
    fun resumeGame() {
        if (_gameState.value is GameState.Paused) {
            _gameState.value = GameState.Playing
            startGameLoop()
        }
    }

    /**
     * Move piece left
     */
    fun moveLeft() {
        val piece = _currentPiece.value ?: return
        if (_gameState.value !is GameState.Playing) return

        if (!board.checkCollision(piece, offsetX = -1)) {
            _currentPiece.value = piece.copy(x = piece.x - 1)
        }
    }

    /**
     * Move piece right
     */
    fun moveRight() {
        val piece = _currentPiece.value ?: return
        if (_gameState.value !is GameState.Playing) return

        if (!board.checkCollision(piece, offsetX = 1)) {
            _currentPiece.value = piece.copy(x = piece.x + 1)
        }
    }

    /**
     * Move piece down (soft drop)
     */
    fun moveDown(): Boolean {
        val piece = _currentPiece.value ?: return false
        if (_gameState.value !is GameState.Playing) return false

        if (!board.checkCollision(piece, offsetY = 1)) {
            _currentPiece.value = piece.copy(y = piece.y + 1)
            // Award 1 point for soft drop
            _stats.value = _stats.value.copy(score = _stats.value.score + 1)
            return true
        } else {
            lockPiece()
            return false
        }
    }

    /**
     * Rotate piece clockwise
     * SNES Tetris uses Nintendo Rotation System - no wall kicks!
     */
    fun rotatePiece() {
        val piece = _currentPiece.value ?: return
        if (_gameState.value !is GameState.Playing) return

        // O piece doesn't rotate in Nintendo Rotation System
        if (piece.type == TetrominoType.O) return

        val rotated = piece.rotated()

        // Nintendo Rotation System: No wall kicks - rotation fails if there's a collision
        if (!board.checkCollision(rotated)) {
            _currentPiece.value = rotated
        }
        // If collision detected, rotation simply fails (no kick attempts)
    }

    /**
     * Hard drop - instantly drop piece to bottom
     * Note: SNES Tetris didn't have hard drop, but we keep it without score bonus
     */
    fun hardDrop() {
        val piece = _currentPiece.value ?: return
        if (_gameState.value !is GameState.Playing) return

        var dropDistance = 0
        while (!board.checkCollision(piece, offsetY = dropDistance + 1)) {
            dropDistance++
        }

        _currentPiece.value = piece.copy(y = piece.y + dropDistance)
        // SNES Tetris: No hard drop bonus (keeping soft drop points only)
        _stats.value = _stats.value.copy(score = _stats.value.score + dropDistance)
        lockPiece()
    }

    /**
     * Return to menu
     */
    fun returnToMenu() {
        gameLoopJob?.cancel()
        _gameState.value = GameState.Menu
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        scope.cancel()
    }

    // Private helper methods

    private fun startGameLoop() {
        lastDropTime = System.currentTimeMillis()

        gameLoopJob = scope.launch {
            while (isActive && _gameState.value is GameState.Playing) {
                val currentTime = System.currentTimeMillis()
                val dropSpeed = _stats.value.dropSpeed()

                if (currentTime - lastDropTime >= dropSpeed) {
                    withContext(Dispatchers.Main) {
                        moveDown()
                    }
                    lastDropTime = currentTime
                }

                // Update board state for rendering
                _boardState.value = board.getGridCopy()

                delay(16) // ~60 FPS
            }
        }
    }

    private fun spawnPiece(): Tetromino {
        val type = TetrominoType.random()
        val color = colorScheme[type] ?: Color.White
        val tetromino = Tetromino.create(type, color)

        // Center horizontally
        val startX = (board.width - tetromino.shape[0].size) / 2
        return tetromino.copy(x = startX, y = 0)
    }

    private fun spawnNextPiece() {
        _currentPiece.value = _nextPiece.value
        _nextPiece.value = spawnPiece()

        // Check if game over (collision at spawn position)
        val piece = _currentPiece.value
        if (piece != null && board.checkCollision(piece)) {
            val stats = _stats.value
            _gameState.value = GameState.GameOver(
                score = stats.score,
                level = stats.level,
                lines = stats.linesCleared
            )
            gameLoopJob?.cancel()
        }
    }

    private fun lockPiece() {
        val piece = _currentPiece.value ?: return

        board.lockTetromino(piece)
        val linesCleared = board.clearLines()

        if (linesCleared > 0) {
            updateStats(linesCleared)
        }

        spawnNextPiece()
    }

    private fun updateStats(linesCleared: Int) {
        val currentStats = _stats.value

        // SNES Tetris Scoring: 40/100/300/800 × (level + 1)
        val lineScore = when (linesCleared) {
            1 -> 40
            2 -> 100
            3 -> 300
            4 -> 800
            else -> 0
        }

        val newScore = currentStats.score + lineScore * (currentStats.level + 1)
        val newLinesCleared = currentStats.linesCleared + linesCleared

        // Level up every 10 lines
        val newLevel = newLinesCleared / 10 + 1

        _stats.value = GameStats(
            score = newScore,
            level = newLevel,
            linesCleared = newLinesCleared
        )
    }
}
