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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tetris.game.Tetromino
import com.tetris.game.TetrominoType
import com.tetris.ui.theme.TetrisTheme

/**
 * Renders the Tetris game board with dynamic sizing
 */
@Composable
fun GameBoard(
    board: List<List<Color?>>,
    currentPiece: Tetromino?,
    theme: TetrisTheme,
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    val boardWidth = 10
    val boardHeight = 20
    val aspectRatio = boardWidth.toFloat() / boardHeight.toFloat()

    val context = LocalContext.current

    // Load background image if available
    val backgroundDrawable = try {
        ContextCompat.getDrawable(context,
            context.resources.getIdentifier("game_background", "drawable", context.packageName))
    } catch (e: Exception) {
        null
    }

    // Load block images for each type
    val blockDrawables = if (useGraphics) {
        TetrominoType.values().associateWith { type ->
            try {
                val resourceName = "block_${type.name.lowercase()}"
                val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                if (resourceId != 0) {
                    ContextCompat.getDrawable(context, resourceId)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    } else {
        emptyMap()
    }

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

            // Draw background image if available
            backgroundDrawable?.let { drawable ->
                drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawContext.canvas.nativeCanvas.apply {
                    drawable.draw(this)
                }
            }

            // Draw locked blocks
            board.forEachIndexed { y, row ->
                row.forEachIndexed { x, color ->
                    if (color != null) {
                        // Try to find the matching tetromino type by color
                        val tetrominoType = findTetrominoTypeByColor(color, theme.shapeColors)
                        val drawable = tetrominoType?.let { blockDrawables[it] }

                        if (useGraphics && drawable != null) {
                            // Draw block image
                            val left = (x * blockSizePx).toInt()
                            val top = (y * blockSizePx).toInt()
                            val right = left + blockSizePx.toInt()
                            val bottom = top + blockSizePx.toInt()

                            drawable.setBounds(left, top, right, bottom)
                            drawContext.canvas.nativeCanvas.apply {
                                drawable.draw(this)
                            }
                        } else {
                            // Fallback to colored rectangles
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
            }

            // Draw current piece
            currentPiece?.let { piece ->
                val drawable = if (useGraphics) blockDrawables[piece.type] else null

                piece.shape.forEachIndexed { row, line ->
                    line.forEachIndexed { col, cell ->
                        if (cell != 0) {
                            val x = piece.x + col
                            val y = piece.y + row
                            if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                                if (useGraphics && drawable != null) {
                                    // Draw block image
                                    val left = (x * blockSizePx).toInt()
                                    val top = (y * blockSizePx).toInt()
                                    val right = left + blockSizePx.toInt()
                                    val bottom = top + blockSizePx.toInt()

                                    drawable.setBounds(left, top, right, bottom)
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawable.draw(this)
                                    }
                                } else {
                                    // Fallback to colored rectangles
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
}

/**
 * Renders the next piece preview
 */
@Composable
fun NextPiecePreview(
    nextPiece: Tetromino?,
    theme: TetrisTheme,
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    val blockSize = 15.dp
    val context = LocalContext.current

    // Load block image for the next piece
    val blockDrawable = if (useGraphics && nextPiece != null) {
        try {
            val resourceName = "block_${nextPiece.type.name.lowercase()}"
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            if (resourceId != 0) {
                ContextCompat.getDrawable(context, resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }

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
                        if (useGraphics && blockDrawable != null) {
                            // Draw block image
                            val left = (col * blockSizePx).toInt()
                            val top = (row * blockSizePx).toInt()
                            val right = left + blockSizePx.toInt()
                            val bottom = top + blockSizePx.toInt()

                            blockDrawable.setBounds(left, top, right, bottom)
                            drawContext.canvas.nativeCanvas.apply {
                                blockDrawable.draw(this)
                            }
                        } else {
                            // Fallback to colored rectangles
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
}

/**
 * Helper function to find TetrominoType based on color from the theme
 */
private fun findTetrominoTypeByColor(color: Color, colorMap: Map<TetrominoType, Color>): TetrominoType? {
    return colorMap.entries.firstOrNull { it.value == color }?.key
}
