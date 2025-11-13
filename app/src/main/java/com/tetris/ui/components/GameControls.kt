package com.tetris.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Simplified touch controls for the game - 4 buttons only
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
    val context = LocalContext.current

    // Load button graphics if available
    val buttonLeftRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_left", "drawable", context.packageName)
        } catch (e: Exception) { 0 }
    } else 0

    val buttonRightRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_right", "drawable", context.packageName)
        } catch (e: Exception) { 0 }
    } else 0

    val buttonDownRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_down", "drawable", context.packageName)
        } catch (e: Exception) { 0 }
    } else 0

    val buttonRotateRes = if (useGraphics) {
        try {
            context.resources.getIdentifier("button_rotate", "drawable", context.packageName)
        } catch (e: Exception) { 0 }
    } else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left button
        ControlButton(
            text = "◀",
            imageRes = buttonLeftRes,
            onClick = onMoveLeft,
            theme = theme,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Down button with hold functionality
        HoldableDownButton(
            imageRes = buttonDownRes,
            theme = theme,
            onMoveDown = onMoveDown,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Rotate button
        ControlButton(
            text = "⟲",
            imageRes = buttonRotateRes,
            onClick = onRotate,
            theme = theme,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Right button
        ControlButton(
            text = "▶",
            imageRes = buttonRightRes,
            onClick = onMoveRight,
            theme = theme,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    theme: TetrisTheme,
    modifier: Modifier = Modifier,
    imageRes: Int = 0
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.gridBorder,
            contentColor = theme.textPrimary
        )
    ) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
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
    modifier: Modifier = Modifier,
    imageRes: Int = 0
) {
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

    Button(
        onClick = {
            // Single tap also moves down once
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
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Down",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "▼",
                fontSize = 32.sp
            )
        }
    }
}
