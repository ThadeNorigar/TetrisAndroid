package com.tetris.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tetris.game.Tetromino
import com.tetris.ui.theme.TetrisTheme

/**
 * Renders the Tetris game board with dynamic sizing
 */
@Composable
fun GameBoard(
    board: List<List<Color?>>,
    currentPiece: Tetromino?,
    theme: TetrisTheme,
    modifier: Modifier = Modifier
) {
    val boardWidth = 10
    val boardHeight = 20
    val aspectRatio = boardWidth.toFloat() / boardHeight.toFloat()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate the optimal size to fit the available space
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        val calculatedHeight = maxWidth / aspectRatio
        val calculatedWidth = maxHeight * aspectRatio

        val (finalWidth, finalHeight) = if (calculatedHeight <= maxHeight) {
            maxWidth to calculatedHeight
        } else {
            calculatedWidth to maxHeight
        }

        Canvas(
            modifier = Modifier
                .size(width = finalWidth, height = finalHeight)
                .border(3.dp, theme.gridBorder)
        ) {
            val blockSizePx = size.width / boardWidth

            // Draw locked blocks
            board.forEachIndexed { y, row ->
                row.forEachIndexed { x, color ->
                    if (color != null) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x * blockSizePx, y * blockSizePx),
                            size = Size(blockSizePx - 2, blockSizePx - 2)
                        )
                        // Border
                        drawRect(
                            color = theme.blockBorder,
                            topLeft = Offset(x * blockSizePx, y * blockSizePx),
                            size = Size(blockSizePx - 2, blockSizePx - 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }
            }

            // Draw current piece
            currentPiece?.let { piece ->
                piece.shape.forEachIndexed { row, line ->
                    line.forEachIndexed { col, cell ->
                        if (cell != 0) {
                            val x = piece.x + col
                            val y = piece.y + row
                            if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                                drawRect(
                                    color = piece.color,
                                    topLeft = Offset(x * blockSizePx, y * blockSizePx),
                                    size = Size(blockSizePx - 2, blockSizePx - 2)
                                )
                                // Border
                                drawRect(
                                    color = theme.blockBorder,
                                    topLeft = Offset(x * blockSizePx, y * blockSizePx),
                                    size = Size(blockSizePx - 2, blockSizePx - 2),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the next piece preview
 */
@Composable
fun NextPiecePreview(
    nextPiece: Tetromino?,
    theme: TetrisTheme,
    modifier: Modifier = Modifier
) {
    val blockSize = 15.dp

    Canvas(
        modifier = modifier
            .size(width = blockSize * 4, height = blockSize * 4)
            .border(2.dp, theme.gridBorder)
    ) {
        nextPiece?.let { piece ->
            val blockSizePx = blockSize.toPx()

            piece.shape.forEachIndexed { row, line ->
                line.forEachIndexed { col, cell ->
                    if (cell != 0) {
                        drawRect(
                            color = piece.color,
                            topLeft = Offset(col * blockSizePx, row * blockSizePx),
                            size = Size(blockSizePx - 2, blockSizePx - 2)
                        )
                        // Border
                        drawRect(
                            color = theme.blockBorder,
                            topLeft = Offset(col * blockSizePx, row * blockSizePx),
                            size = Size(blockSizePx - 2, blockSizePx - 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                    }
                }
            }
        }
    }
}
