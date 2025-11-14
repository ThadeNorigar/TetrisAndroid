package com.tetris.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.ui.theme.TetrisTheme

/**
 * Main menu screen
 */
@Composable
fun MenuScreen(
    theme: TetrisTheme,
    highScore: Int,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    val context = LocalContext.current

    // Load fullscreen menu background image if available
    val menuBackgroundResourceId = try {
        context.resources.getIdentifier("menu_background", "drawable", context.packageName)
    } catch (e: Exception) {
        0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Draw fullscreen background image if available
        if (useGraphics && menuBackgroundResourceId != 0) {
            Image(
                painter = painterResource(id = menuBackgroundResourceId),
                contentDescription = "Menu Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // Menu buttons
        MenuButton(
            text = "START GAME",
            onClick = onStartGame,
            theme = theme,
            highlighted = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // High Score (dezent)
        if (highScore > 0) {
            Text(
                text = "HIGH SCORE: $highScore",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = theme.textSecondary
            )
        }
        }
    }
}

/**
 * Options screen
 */
@Composable
fun OptionsScreen(
    currentTheme: TetrisTheme,
    availableThemes: List<TetrisTheme>,
    onThemeSelected: (TetrisTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OPTIONS",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.textHighlight
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "THEME",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Theme selection
        availableThemes.forEach { theme ->
            val isSelected = theme.name == currentTheme.name
            ThemeOption(
                themeName = theme.name,
                isSelected = isSelected,
                theme = currentTheme,
                onClick = { onThemeSelected(theme) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(
            text = "BACK",
            onClick = onBack,
            theme = currentTheme
        )
    }
}

@Composable
private fun ThemeOption(
    themeName: String,
    isSelected: Boolean,
    theme: TetrisTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) theme.textHighlight.copy(alpha = 0.2f)
                else theme.gridBorder.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Text(
                    text = "▶ ",
                    color = theme.textHighlight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = themeName,
                fontSize = 20.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) theme.textHighlight else theme.textPrimary
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit,
    theme: TetrisTheme,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    val context = LocalContext.current

    // Load button graphic based on text
    val buttonType = when (text) {
        "START GAME" -> "button_start_game"
        "BACK" -> "button_back"
        else -> null
    }

    val buttonResourceId = if (useGraphics && buttonType != null) {
        try {
            context.resources.getIdentifier(buttonType, "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    } else {
        0
    }

    if (useGraphics && buttonResourceId != 0) {
        // Use custom graphics
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = buttonResourceId),
                contentDescription = text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        // Fallback to Material3 button
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (highlighted) theme.textHighlight else theme.gridBorder,
                contentColor = if (highlighted) theme.background else theme.textPrimary
            )
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
