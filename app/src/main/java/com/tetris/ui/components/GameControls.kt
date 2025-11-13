package com.tetris.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.ui.theme.TetrisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Touch controls for the game - 5 buttons
 */
@Composable
fun GameControls(
    theme: TetrisTheme,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left button
        ControlButton(
            text = "◀",
            buttonType = "button_left",
            onClick = onMoveLeft,
            theme = theme,
            useGraphics = useGraphics,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Down button with hold functionality
        HoldableDownButton(
            theme = theme,
            onMoveDown = onMoveDown,
            useGraphics = useGraphics,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Hard Drop button
        ControlButton(
            text = "⬇",
            buttonType = "button_hard_drop",
            onClick = onHardDrop,
            theme = theme,
            useGraphics = useGraphics,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Rotate button
        ControlButton(
            text = "⟲",
            buttonType = "button_rotate",
            onClick = onRotate,
            theme = theme,
            useGraphics = useGraphics,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Right button
        ControlButton(
            text = "▶",
            buttonType = "button_right",
            onClick = onMoveRight,
            theme = theme,
            useGraphics = useGraphics,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlButton(
    text: String,
    buttonType: String,
    onClick: () -> Unit,
    theme: TetrisTheme,
    useGraphics: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Load button graphics if available
    val buttonNormalRes = if (useGraphics) {
        try {
            context.resources.getIdentifier(buttonType, "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    } else {
        0
    }

    val buttonPressedRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("${buttonType}_pressed", "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    } else {
        0
    }

    val hasGraphics = buttonNormalRes != 0

    if (hasGraphics) {
        // Use custom graphics
        Box(
            modifier = modifier
                .height(72.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val imageRes = if (isPressed && buttonPressedRes != 0) buttonPressedRes else buttonNormalRes
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        // Fallback to Material3 button with text
        Button(
            onClick = onClick,
            modifier = modifier.height(72.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.gridBorder,
                contentColor = theme.textPrimary
            ),
            interactionSource = interactionSource
        ) {
            Text(
                text = text,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun HoldableDownButton(
    theme: TetrisTheme,
    onMoveDown: () -> Unit,
    useGraphics: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            // Initial delay before fast drop starts
            delay(200)
            // Continue fast dropping while held
            while (isPressed) {
                onMoveDown()
                delay(50) // Fast drop interval
            }
        }
    }

    // Load button graphics if available
    val buttonNormalRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_down", "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    } else {
        0
    }

    val buttonPressedRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_down_pressed", "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    } else {
        0
    }

    val hasGraphics = buttonNormalRes != 0

    if (hasGraphics) {
        // Use custom graphics
        Box(
            modifier = modifier
                .height(72.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isPressed) {
                        onMoveDown()
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val imageRes = if (isPressed && buttonPressedRes != 0) buttonPressedRes else buttonNormalRes
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Down",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        // Fallback to Material3 button with text
        Button(
            onClick = {
                if (!isPressed) {
                    onMoveDown()
                }
            },
            modifier = modifier
                .height(72.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPressed) theme.textHighlight else theme.gridBorder,
                contentColor = if (isPressed) theme.background else theme.textPrimary
            )
        ) {
            Text(
                text = "▼",
                fontSize = 32.sp
            )
        }
    }
}
