package com.tetris.game

import androidx.compose.ui.graphics.Color

/**
 * Represents a Tetromino piece (the falling blocks in Tetris)
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
     * Nintendo Rotation System: I and O pieces have special rotation behavior
     */
    fun rotated(): Tetromino {
        // O piece doesn't rotate
        if (type == TetrominoType.O) return this

        val n = shape.size
        val m = shape[0].size
        val rotated = List(m) { col ->
            List(n) { row ->
                shape[n - 1 - row][col]
            }
        }

        // I piece has only 2 rotation states (horizontal and vertical)
        // If rotating back to horizontal from vertical, return original orientation
        if (type == TetrominoType.I && rotated.size == 1 && rotated[0].size == 4) {
            // This is the horizontal state - return it as-is
            return copy(shape = rotated)
        } else if (type == TetrominoType.I && rotated.size == 4 && rotated[0].size == 1) {
            // This is the vertical state - return it as-is
            return copy(shape = rotated)
        }

        return copy(shape = rotated)
    }

    companion object {
        /**
         * Create tetromino with SNES Tetris (Nintendo Rotation System) spawn orientations
         */
        fun create(type: TetrominoType, color: Color): Tetromino {
            val shape = when (type) {
                // I: Spawns horizontal (2 rotation states only)
                TetrominoType.I -> listOf(listOf(1, 1, 1, 1))
                // O: Never rotates
                TetrominoType.O -> listOf(
                    listOf(1, 1),
                    listOf(1, 1)
                )
                // T: 4 rotation states, centered on middle block
                TetrominoType.T -> listOf(
                    listOf(0, 1, 0),
                    listOf(1, 1, 1)
                )
                // S: 4 rotation states
                TetrominoType.S -> listOf(
                    listOf(0, 1, 1),
                    listOf(1, 1, 0)
                )
                // Z: 4 rotation states
                TetrominoType.Z -> listOf(
                    listOf(1, 1, 0),
                    listOf(0, 1, 1)
                )
                // J: 4 rotation states, centered on middle block
                TetrominoType.J -> listOf(
                    listOf(1, 0, 0),
                    listOf(1, 1, 1)
                )
                // L: 4 rotation states, centered on middle block
                TetrominoType.L -> listOf(
                    listOf(0, 0, 1),
                    listOf(1, 1, 1)
                )
            }
            return Tetromino(type, shape, color, x = 0, y = 0)
        }
    }
}

enum class TetrominoType {
    I, O, T, S, Z, J, L;

    companion object {
        fun random() = values().random()
    }
}
