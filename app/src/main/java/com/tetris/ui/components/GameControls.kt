package com.tetris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.ui.theme.TetrisTheme
import kotlin.math.abs

/**
 * Touch controls for the game
 */
@Composable
fun GameControls(
    theme: TetrisTheme,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var hasSwiped by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        hasSwiped = false
                    },
                    onDrag = { change, dragAmount ->
                        if (!hasSwiped) {
                            val start = dragStart ?: return@detectDragGestures
                            val current = change.position
                            val dx = current.x - start.x
                            val dy = current.y - start.y

                            // Swipe threshold
                            if (abs(dx) > 50 || abs(dy) > 50) {
                                hasSwiped = true
                                when {
                                    abs(dx) > abs(dy) -> {
                                        if (dx > 0) onMoveRight() else onMoveLeft()
                                    }
                                    dy > 0 -> onMoveDown()
                                    else -> onRotate()
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        dragStart = null
                        hasSwiped = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onRotate()
                    },
                    onDoubleTap = {
                        onHardDrop()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👆 Tap: Rotate | ⬆️ Swipe Up: Rotate",
            color = theme.textSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "⬅️➡️ Swipe: Move | ⬇️ Swipe Down: Drop",
            color = theme.textSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "👆👆 Double Tap: Hard Drop",
            color = theme.textHighlight,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Button controls (alternative)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left button
            ControlButton(
                text = "◀",
                onClick = onMoveLeft,
                theme = theme
            )

            // Rotate button
            ControlButton(
                text = "🔄",
                onClick = onRotate,
                theme = theme
            )

            // Right button
            ControlButton(
                text = "▶",
                onClick = onMoveRight,
                theme = theme
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Down button
            ControlButton(
                text = "▼",
                onClick = onMoveDown,
                theme = theme
            )

            // Hard drop button
            ControlButton(
                text = "⬇",
                onClick = onHardDrop,
                theme = theme,
                highlighted = true
            )
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    theme: TetrisTheme,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlighted) theme.textHighlight else theme.gridBorder,
            contentColor = if (highlighted) theme.background else theme.textPrimary
        )
    ) {
        Text(
            text = text,
            fontSize = 24.sp
        )
    }
}
