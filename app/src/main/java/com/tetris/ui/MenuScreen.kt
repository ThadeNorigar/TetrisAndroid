package com.tetris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "RETRO TETRIS",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Classic Block Puzzle",
            fontSize = 20.sp,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // High Score
        if (highScore > 0) {
            Text(
                text = "HIGH SCORE: $highScore",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Menu buttons
        MenuButton(
            text = "START GAME",
            onClick = onStartGame,
            theme = theme,
            highlighted = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        MenuButton(
            text = "OPTIONS",
            onClick = onOptions,
            theme = theme
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Instructions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "HOW TO PLAY:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
            Text(
                text = "Swipe Left/Right: Move",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
            Text(
                text = "Tap or Swipe Up: Rotate",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
            Text(
                text = "Double Tap: Hard Drop",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
            Text(
                text = "Swipe Down: Soft Drop",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
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
    modifier: Modifier = Modifier
) {
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
