package com.tetris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.GameState
import com.tetris.game.GameStats
import com.tetris.game.Tetromino
import com.tetris.ui.components.GameBoard
import com.tetris.ui.components.GameControls
import com.tetris.ui.components.NextPiecePreview
import com.tetris.ui.theme.TetrisTheme
import androidx.compose.ui.graphics.Color

/**
 * Main game screen
 */
@Composable
fun GameScreen(
    gameState: GameState,
    stats: GameStats,
    board: List<List<Color?>>,
    currentPiece: Tetromino?,
    nextPiece: Tetromino?,
    theme: TetrisTheme,
    highScore: Int,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        when (gameState) {
            is GameState.Playing -> {
                PlayingScreen(
                    stats = stats,
                    board = board,
                    currentPiece = currentPiece,
                    nextPiece = nextPiece,
                    theme = theme,
                    highScore = highScore,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onMoveDown = onMoveDown,
                    onRotate = onRotate,
                    onHardDrop = onHardDrop,
                    onPause = onPause
                )
            }
            is GameState.Paused -> {
                PausedOverlay(
                    theme = theme,
                    onResume = onResume,
                    onBackToMenu = onBackToMenu
                )
            }
            is GameState.GameOver -> {
                GameOverScreen(
                    score = gameState.score,
                    level = gameState.level,
                    lines = gameState.lines,
                    highScore = highScore,
                    theme = theme,
                    onBackToMenu = onBackToMenu
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun PlayingScreen(
    stats: GameStats,
    board: List<List<Color?>>,
    currentPiece: Tetromino?,
    nextPiece: Tetromino?,
    theme: TetrisTheme,
    highScore: Int,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 14.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = "${stats.score}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "HIGH",
                    fontSize = 14.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = "$highScore",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textHighlight
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LEVEL: ${stats.level}",
                fontSize = 18.sp,
                color = theme.textPrimary
            )
            Text(
                text = "LINES: ${stats.linesCleared}",
                fontSize = 18.sp,
                color = theme.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game board and next piece
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            GameBoard(
                board = board,
                currentPiece = currentPiece,
                theme = theme
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NEXT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                NextPiecePreview(
                    nextPiece = nextPiece,
                    theme = theme
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pause button
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.gridBorder,
                        contentColor = theme.textPrimary
                    )
                ) {
                    Text("⏸ PAUSE")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        GameControls(
            theme = theme,
            onMoveLeft = onMoveLeft,
            onMoveRight = onMoveRight,
            onMoveDown = onMoveDown,
            onRotate = onRotate,
            onHardDrop = onHardDrop
        )
    }
}

@Composable
private fun PausedOverlay(
    theme: TetrisTheme,
    onResume: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.overlay.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "PAUSED",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textHighlight
            )

            Button(
                onClick = onResume,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.textHighlight,
                    contentColor = theme.background
                )
            ) {
                Text(
                    text = "RESUME",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onBackToMenu,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.gridBorder,
                    contentColor = theme.textPrimary
                )
            ) {
                Text(
                    text = "MENU",
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun GameOverScreen(
    score: Int,
    level: Int,
    lines: Int,
    highScore: Int,
    theme: TetrisTheme,
    onBackToMenu: () -> Unit
) {
    val isNewHighScore = score > highScore

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.overlay.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "GAME OVER",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textDanger
            )

            if (isNewHighScore) {
                Text(
                    text = "🏆 NEW HIGH SCORE! 🏆",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textHighlight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("SCORE", score.toString(), theme, highlight = isNewHighScore)
                StatRow("LEVEL", level.toString(), theme)
                StatRow("LINES", lines.toString(), theme)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackToMenu,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.textHighlight,
                    contentColor = theme.background
                )
            ) {
                Text(
                    text = "MENU",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    theme: TetrisTheme,
    highlight: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 20.sp,
            color = theme.textSecondary
        )
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) theme.textHighlight else theme.textPrimary
        )
    }
}
