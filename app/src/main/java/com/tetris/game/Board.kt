package com.tetris.game

import androidx.compose.ui.graphics.Color

/**
 * Represents the Tetris game board
 */
class Board(
    val width: Int = 10,
    val height: Int = 20
) {
    // Grid stores colors of locked blocks
    private val grid: MutableList<MutableList<Color?>> = MutableList(height) {
        MutableList(width) { null }
    }

    /**
     * Get the color at a specific position
     */
    fun getCell(x: Int, y: Int): Color? {
        if (x !in 0 until width || y !in 0 until height) return null
        return grid[y][x]
    }

    /**
     * Check if a position is occupied
     */
    fun isOccupied(x: Int, y: Int): Boolean {
        return x !in 0 until width || y !in 0 until height || grid[y][x] != null
    }

    /**
     * Check if a tetromino collides with the board
     */
    fun checkCollision(tetromino: Tetromino, offsetX: Int = 0, offsetY: Int = 0): Boolean {
        tetromino.shape.forEachIndexed { row, line ->
            line.forEachIndexed { col, cell ->
                if (cell != 0) {
                    val x = tetromino.x + col + offsetX
                    val y = tetromino.y + row + offsetY

                    // Check boundaries
                    if (x < 0 || x >= width || y >= height) return true

                    // Check collision with existing blocks (only if y >= 0)
                    if (y >= 0 && grid[y][x] != null) return true
                }
            }
        }
        return false
    }

    /**
     * Lock a tetromino into the board
     */
    fun lockTetromino(tetromino: Tetromino) {
        tetromino.shape.forEachIndexed { row, line ->
            line.forEachIndexed { col, cell ->
                if (cell != 0) {
                    val x = tetromino.x + col
                    val y = tetromino.y + row
                    if (y >= 0 && y < height && x >= 0 && x < width) {
                        grid[y][x] = tetromino.color
                    }
                }
            }
        }
    }

    /**
     * Clear completed lines and return the number of lines cleared
     */
    fun clearLines(): Int {
        val linesToClear = mutableListOf<Int>()

        // Find completed lines
        for (y in 0 until height) {
            if (grid[y].all { it != null }) {
                linesToClear.add(y)
            }
        }

        // Remove completed lines
        linesToClear.sortedDescending().forEach { y ->
            grid.removeAt(y)
            grid.add(0, MutableList(width) { null })
        }

        return linesToClear.size
    }

    /**
     * Reset the board
     */
    fun reset() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = null
            }
        }
    }

    /**
     * Create a copy of the grid for rendering
     */
    fun getGridCopy(): List<List<Color?>> {
        return grid.map { it.toList() }
    }
}
