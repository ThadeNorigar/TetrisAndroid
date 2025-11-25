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

    // Lock for thread-safe grid access
    private val gridLock = Any()

    /**
     * Get the color at a specific position
     */
    fun getCell(x: Int, y: Int): Color? {
        if (x !in 0 until width || y !in 0 until height) return null
        synchronized(gridLock) {
            return grid[y][x]
        }
    }

    /**
     * Check if a position is occupied
     */
    fun isOccupied(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return true
        synchronized(gridLock) {
            return grid[y][x] != null
        }
    }

    /**
     * Check if a tetromino collides with the board
     */
    fun checkCollision(tetromino: Tetromino, offsetX: Int = 0, offsetY: Int = 0): Boolean {
        synchronized(gridLock) {
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
    }

    /**
     * Lock a tetromino into the board
     */
    fun lockTetromino(tetromino: Tetromino) {
        synchronized(gridLock) {
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
    }

    /**
     * Find completed lines without clearing them
     */
    fun findCompletedLines(): List<Int> {
        synchronized(gridLock) {
            val linesToClear = mutableListOf<Int>()
            for (y in 0 until height) {
                if (grid[y].all { it != null }) {
                    linesToClear.add(y)
                }
            }
            return linesToClear
        }
    }

    /**
     * Clear completed lines and return the number of lines cleared
     */
    fun clearLines(): Int {
        synchronized(gridLock) {
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
    }

    /**
     * Reset the board
     */
    fun reset() {
        synchronized(gridLock) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    grid[y][x] = null
                }
            }
        }
    }

    /**
     * Create a copy of the grid for rendering
     */
    fun getGridCopy(): List<List<Color?>> {
        synchronized(gridLock) {
            return grid.map { it.toList() }
        }
    }

    /**
     * Add garbage lines from the bottom (for multiplayer)
     * Returns true if successful, false if it would cause game over
     */
    fun addGarbageLines(count: Int, garbageColor: Color = Color.Gray): Boolean {
        synchronized(gridLock) {
            if (count <= 0) return true

            // Check if adding garbage lines would overflow the board
            // Count how many rows from the top are empty
            var emptyRowsFromTop = 0
            for (y in 0 until height) {
                if (grid[y].all { it == null }) {
                    emptyRowsFromTop++
                } else {
                    break
                }
            }

            if (emptyRowsFromTop < count) {
                // Not enough space, would cause game over
                return false
            }

            // Remove top rows
            repeat(count) {
                grid.removeAt(0)
            }

            // Add garbage lines at bottom with one random gap per line
            repeat(count) {
                val garbageLine = MutableList<Color?>(width) { garbageColor }
                // Add a random gap in each garbage line
                val gapPosition = (0 until width).random()
                garbageLine[gapPosition] = null
                grid.add(garbageLine)
            }

            return true
        }
    }
}
