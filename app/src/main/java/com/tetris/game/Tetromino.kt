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
     */
    fun rotated(): Tetromino {
        val n = shape.size
        val m = shape[0].size
        val rotated = List(m) { col ->
            List(n) { row ->
                shape[n - 1 - row][col]
            }
        }
        return copy(shape = rotated)
    }

    companion object {
        fun create(type: TetrominoType, color: Color): Tetromino {
            val shape = when (type) {
                TetrominoType.I -> listOf(listOf(1, 1, 1, 1))
                TetrominoType.O -> listOf(
                    listOf(1, 1),
                    listOf(1, 1)
                )
                TetrominoType.T -> listOf(
                    listOf(0, 1, 0),
                    listOf(1, 1, 1)
                )
                TetrominoType.S -> listOf(
                    listOf(0, 1, 1),
                    listOf(1, 1, 0)
                )
                TetrominoType.Z -> listOf(
                    listOf(1, 1, 0),
                    listOf(0, 1, 1)
                )
                TetrominoType.J -> listOf(
                    listOf(1, 0, 0),
                    listOf(1, 1, 1)
                )
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
