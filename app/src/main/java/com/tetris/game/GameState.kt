package com.tetris.game

/**
 * Represents the current state of the game
 */
sealed class GameState {
    object Menu : GameState()
    object Playing : GameState()
    object Paused : GameState()
    data class GameOver(val score: Int, val level: Int, val lines: Int) : GameState()
}

/**
 * Game statistics
 */
data class GameStats(
    val score: Int = 0,
    val level: Int = 1,
    val linesCleared: Int = 0
) {
    /**
     * Calculate drop speed in milliseconds based on level
     */
    fun dropSpeed(): Long {
        return maxOf(100L, 500L - (level - 1) * 50L)
    }
}
