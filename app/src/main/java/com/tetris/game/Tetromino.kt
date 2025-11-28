package com.tetris.game

import androidx.compose.ui.graphics.Color

/**
 * Represents a Tetromino piece (the falling blocks in Tetris)
 * SNES Tetris rotation system implementation
 */
data class Tetromino(
    val type: TetrominoType,
    val shape: List<List<Int>>,
    val color: Color,
    var x: Int = 0,
    var y: Int = 0
) {
    /**
     * Rotate the tetromino 90 degrees clockwise
     * SNES Tetris rotation system:
     * - O piece: Never rotates
     * - I piece: Rotates on 4x4 field around center
     * - Other pieces: Rotate on 3x3 field around center
     */
    fun rotated(): Tetromino {
        // O piece doesn't rotate
        if (type == TetrominoType.O) return this

        val n = shape.size
        val m = shape[0].size

        // Rotate 90 degrees clockwise: transpose and reverse each row
        val rotated = List(m) { col ->
            List(n) { row ->
                shape[n - 1 - row][col]
            }
        }

        return copy(shape = rotated)
    }

    companion object {
        /**
         * Create tetromino with SNES Tetris (Nintendo Rotation System) spawn orientations
         * All pieces spawn in their "flat" orientation
         */
        fun create(type: TetrominoType, color: Color): Tetromino {
            val shape = when (type) {
                // I: 4x4 field, horizontal in row 2 (index 1)
                TetrominoType.I -> listOf(
                    listOf(0, 0, 0, 0),
                    listOf(1, 1, 1, 1),
                    listOf(0, 0, 0, 0),
                    listOf(0, 0, 0, 0)
                )

                // O: 2x2, never rotates (not on 3x3 or 4x4 grid)
                TetrominoType.O -> listOf(
                    listOf(1, 1),
                    listOf(1, 1)
                )

                // T: 3x3 field, spawns pointing up
                TetrominoType.T -> listOf(
                    listOf(0, 1, 0),
                    listOf(1, 1, 1),
                    listOf(0, 0, 0)
                )

                // S: 3x3 field
                TetrominoType.S -> listOf(
                    listOf(0, 1, 1),
                    listOf(1, 1, 0),
                    listOf(0, 0, 0)
                )

                // Z: 3x3 field
                TetrominoType.Z -> listOf(
                    listOf(1, 1, 0),
                    listOf(0, 1, 1),
                    listOf(0, 0, 0)
                )

                // J: 3x3 field
                TetrominoType.J -> listOf(
                    listOf(1, 0, 0),
                    listOf(1, 1, 1),
                    listOf(0, 0, 0)
                )

                // L: 3x3 field
                TetrominoType.L -> listOf(
                    listOf(0, 0, 1),
                    listOf(1, 1, 1),
                    listOf(0, 0, 0)
                )
            }
            return Tetromino(type, shape, color, x = 0, y = 0)
        }
    }
}

enum class TetrominoType {
    I, O, T, S, Z, J, L, GARBAGE;

    companion object {
        fun random() = values().filter { it != GARBAGE }.random()
    }
}
