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
    // Make board accessible for multiplayer garbage lines
    internal val board = Board()
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

    // Animation states
    private val _hardDropAnimating = MutableStateFlow(false)
    val hardDropAnimating: StateFlow<Boolean> = _hardDropAnimating.asStateFlow()

    private val _lineClearAnimation = MutableStateFlow<Set<Int>>(emptySet())
    val lineClearAnimation: StateFlow<Set<Int>> = _lineClearAnimation.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lastDropTime = 0L
    private var animationJob: Job? = null

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
     * Hard drop - drop piece to bottom with animation
     * Note: SNES Tetris didn't have hard drop, but we keep it without score bonus
     */
    fun hardDrop() {
        val piece = _currentPiece.value ?: return
        if (_gameState.value !is GameState.Playing) return
        if (_hardDropAnimating.value) return // Don't allow multiple hard drops at once

        var dropDistance = 0
        while (!board.checkCollision(piece, offsetY = dropDistance + 1)) {
            dropDistance++
        }

        if (dropDistance == 0) {
            // Already at bottom
            lockPiece()
            return
        }

        // SNES Tetris: No hard drop bonus (keeping soft drop points only)
        _stats.value = _stats.value.copy(score = _stats.value.score + dropDistance)

        // Start animation
        _hardDropAnimating.value = true
        val startY = piece.y
        val endY = piece.y + dropDistance

        animationJob?.cancel()
        animationJob = scope.launch {
            val animationDuration = 150L // 150ms total animation
            val steps = dropDistance
            val delayPerStep = animationDuration / steps.coerceAtLeast(1)

            for (i in 1..dropDistance) {
                if (!isActive) break
                _currentPiece.value = piece.copy(y = startY + i)
                delay(delayPerStep)
            }

            // Ensure final position is set
            _currentPiece.value = piece.copy(y = endY)
            _hardDropAnimating.value = false

            // Lock piece after animation
            withContext(Dispatchers.Main) {
                lockPiece()
            }
        }
    }

    /**
     * Return to menu
     */
    fun returnToMenu() {
        gameLoopJob?.cancel()
        _gameState.value = GameState.Menu
    }

    /**
     * Add garbage lines to the board (for multiplayer)
     * Returns true if successful, false if it causes game over
     */
    fun addGarbageLines(count: Int, garbageColor: Color = Color(0xFF808080)): Boolean {
        val success = board.addGarbageLines(count, garbageColor)
        if (!success) {
            // Adding garbage caused game over
            val stats = _stats.value
            _gameState.value = GameState.GameOver(
                score = stats.score,
                level = stats.level,
                lines = stats.linesCleared
            )
            gameLoopJob?.cancel()
        } else {
            // Update board state for rendering
            _boardState.value = board.getGridCopy()
        }
        return success
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
        // Spawn at y = -1 to allow pieces to enter from above the visible board
        // This prevents false game over when the top row has blocks
        return tetromino.copy(x = startX, y = -1)
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

    private fun spawnSavedPiece(piece: Tetromino?) {
        _currentPiece.value = piece

        // Check if game over (collision at spawn position)
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

        // Save the current next piece (this will become the new current piece)
        val pieceToSpawn = _nextPiece.value

        // Clear current piece to prevent double rendering during animation
        _currentPiece.value = null

        // Generate new next piece immediately so UI shows correct preview
        _nextPiece.value = spawnPiece()

        // Check for completed lines
        val completedLines = board.findCompletedLines()

        if (completedLines.isNotEmpty()) {
            // Start line clear animation
            animateLineClear(completedLines, pieceToSpawn)
        } else {
            // No lines to clear, just spawn the saved next piece
            spawnSavedPiece(pieceToSpawn)
        }
    }

    private fun animateLineClear(linesToClear: List<Int>, pieceToSpawn: Tetromino?) {
        scope.launch {
            // Blink animation - SNES style (3 blinks over ~450ms)
            val blinkCount = 3
            val blinkDuration = 150L // Each blink (on+off) takes 150ms

            repeat(blinkCount) {
                // Show lines (blink on)
                _lineClearAnimation.value = linesToClear.toSet()
                delay(blinkDuration / 2)

                // Hide lines (blink off)
                _lineClearAnimation.value = emptySet()
                delay(blinkDuration / 2)
            }

            // Clear animation state
            _lineClearAnimation.value = emptySet()

            // Actually clear the lines
            val linesCleared = board.clearLines()

            if (linesCleared > 0) {
                updateStats(linesCleared)
            }

            // Update board state
            _boardState.value = board.getGridCopy()

            // Spawn the saved next piece
            withContext(Dispatchers.Main) {
                spawnSavedPiece(pieceToSpawn)
            }
        }
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
