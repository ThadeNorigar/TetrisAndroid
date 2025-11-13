package com.tetris.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
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

    // Load blocks spritesheet
    val blocksSpritesheet = if (useGraphics) {
        try {
            val resourceId = context.resources.getIdentifier("blocks_spritesheet", "drawable", context.packageName)
            if (resourceId != 0) {
                BitmapFactory.decodeResource(context.resources, resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    } else {
        null
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

                        if (useGraphics && blocksSpritesheet != null && tetrominoType != null) {
                            // Draw block from spritesheet
                            drawBlockFromSpritesheet(
                                spritesheet = blocksSpritesheet,
                                tetrominoType = tetrominoType,
                                x = x * blockSizePx,
                                y = y * blockSizePx,
                                size = blockSizePx
                            )
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
                piece.shape.forEachIndexed { row, line ->
                    line.forEachIndexed { col, cell ->
                        if (cell != 0) {
                            val x = piece.x + col
                            val y = piece.y + row
                            if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                                if (useGraphics && blocksSpritesheet != null) {
                                    // Draw block from spritesheet
                                    drawBlockFromSpritesheet(
                                        spritesheet = blocksSpritesheet,
                                        tetrominoType = piece.type,
                                        x = x * blockSizePx,
                                        y = y * blockSizePx,
                                        size = blockSizePx
                                    )
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

    // Load blocks spritesheet
    val blocksSpritesheet = if (useGraphics) {
        try {
            val resourceId = context.resources.getIdentifier("blocks_spritesheet", "drawable", context.packageName)
            if (resourceId != 0) {
                BitmapFactory.decodeResource(context.resources, resourceId)
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
                        if (useGraphics && blocksSpritesheet != null) {
                            // Draw block from spritesheet
                            drawBlockFromSpritesheet(
                                spritesheet = blocksSpritesheet,
                                tetrominoType = piece.type,
                                x = col * blockSizePx,
                                y = row * blockSizePx,
                                size = blockSizePx
                            )
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
 * Helper function to draw a block from the spritesheet
 * Spritesheet layout: Horizontal strip with 7 blocks (I, O, T, S, Z, J, L)
 */
private fun DrawScope.drawBlockFromSpritesheet(
    spritesheet: Bitmap,
    tetrominoType: TetrominoType,
    x: Float,
    y: Float,
    size: Float
) {
    // Calculate the block index (0-6)
    val blockIndex = when (tetrominoType) {
        TetrominoType.I -> 0
        TetrominoType.O -> 1
        TetrominoType.T -> 2
        TetrominoType.S -> 3
        TetrominoType.Z -> 4
        TetrominoType.J -> 5
        TetrominoType.L -> 6
    }

    // Calculate spritesheet dimensions
    val spriteWidth = spritesheet.width / 7 // 7 blocks in horizontal strip
    val spriteHeight = spritesheet.height

    // Source rectangle (portion of spritesheet to draw)
    val srcRect = Rect(
        blockIndex * spriteWidth,
        0,
        (blockIndex + 1) * spriteWidth,
        spriteHeight
    )

    // Destination rectangle (where to draw on canvas)
    val dstRect = android.graphics.RectF(
        x,
        y,
        x + size,
        y + size
    )

    // Draw the block
    drawContext.canvas.nativeCanvas.drawBitmap(
        spritesheet,
        srcRect,
        dstRect,
        null
    )
}

/**
 * Helper function to find TetrominoType based on color from the theme
 */
private fun findTetrominoTypeByColor(color: Color, colorMap: Map<TetrominoType, Color>): TetrominoType? {
    return colorMap.entries.firstOrNull { it.value == color }?.key
}
