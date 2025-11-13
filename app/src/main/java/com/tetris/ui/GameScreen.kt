package com.tetris.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    modifier: Modifier = Modifier,
    useGraphics: Boolean = true
) {
    val context = LocalContext.current

    // Load fullscreen background image if available
    val backgroundResourceId = try {
        context.resources.getIdentifier("screen_background", "drawable", context.packageName)
    } catch (e: Exception) {
        0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Draw fullscreen background image if available
        if (useGraphics && backgroundResourceId != 0) {
            Image(
                painter = painterResource(id = backgroundResourceId),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

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
                    onPause = onPause,
                    useGraphics = useGraphics
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
    onPause: () -> Unit,
    useGraphics: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Game board - takes maximum available space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                board = board,
                currentPiece = currentPiece,
                theme = theme,
                modifier = Modifier.fillMaxSize(),
                useGraphics = useGraphics
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info section: Next piece, Score, Level, Lines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Next piece preview (text and border are in background image)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NextPiecePreview(
                    nextPiece = nextPiece,
                    theme = theme,
                    useGraphics = useGraphics
                )
            }

            // Stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCORE",
                            fontSize = 10.sp,
                            color = theme.textSecondary
                        )
                        Text(
                            text = "${stats.score}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HIGH",
                            fontSize = 10.sp,
                            color = theme.textSecondary
                        )
                        Text(
                            text = "$highScore",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textHighlight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Level and Lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LVL: ${stats.level}",
                        fontSize = 12.sp,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "LINES: ${stats.linesCleared}",
                        fontSize = 12.sp,
                        color = theme.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls at the bottom
        GameControls(
            theme = theme,
            onMoveLeft = onMoveLeft,
            onMoveRight = onMoveRight,
            onMoveDown = onMoveDown,
            onRotate = onRotate,
            onHardDrop = onHardDrop,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
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
