package com.tetris.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetris.game.*
import com.tetris.ui.theme.TetrisTheme
import com.tetris.ui.components.BoardView
import com.tetris.ui.components.PiecePreview

/**
 * Multiplayer game screen showing both players' boards side by side
 */
@Composable
fun MultiplayerGameScreen(
    theme: TetrisTheme,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get NetworkManager from LobbyViewModel (passed through navigation)
    val context = LocalContext.current
    // For now, we'll create a simplified version without proper DI
    // In production, you'd use proper dependency injection

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Text(
            text = "MULTIPLAYER GAME",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left side - Local Player
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOU",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textHighlight
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Simplified board view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.5f)
                        .background(theme.gridBorder.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Score: 0\nLevel: 1\nLines: 0",
                        fontSize = 10.sp,
                        color = theme.textPrimary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    )
                }
            }

            // Right side - Opponent
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OPPONENT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Simplified opponent board view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.5f)
                        .background(theme.gridBorder.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Score: 0\nLevel: 1\nLines: 0",
                        fontSize = 10.sp,
                        color = theme.textPrimary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    )
                }
            }
        }

        // Controls at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { /* Pause */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.gridBorder,
                    contentColor = theme.textPrimary
                )
            ) {
                Text("PAUSE", fontSize = 12.sp)
            }

            Button(
                onClick = onBackToMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.gridBorder,
                    contentColor = theme.textPrimary
                )
            ) {
                Text("QUIT", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Simplified mini board view for multiplayer
 */
@Composable
private fun MiniBoardView(
    boardState: List<List<Color?>>,
    currentPiece: Tetromino?,
    theme: TetrisTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.5f)
            .background(theme.gridBorder.copy(alpha = 0.2f))
    )
}

/**
 * Stats display for multiplayer
 */
@Composable
private fun MiniStatsDisplay(
    stats: GameStats,
    theme: TetrisTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SCORE",
            fontSize = 10.sp,
            color = theme.textSecondary
        )
        Text(
            text = "${stats.score}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "LEVEL",
            fontSize = 10.sp,
            color = theme.textSecondary
        )
        Text(
            text = "${stats.level}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "LINES",
            fontSize = 10.sp,
            color = theme.textSecondary
        )
        Text(
            text = "${stats.linesCleared}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
    }
}

/**
 * Game over dialog for multiplayer
 */
@Composable
private fun MultiplayerGameOverDialog(
    winner: Winner,
    localStats: GameStats,
    theme: TetrisTheme,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = when (winner) {
                    is Winner.LocalPlayer -> "YOU WIN!"
                    is Winner.Opponent -> "YOU LOSE"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = when (winner) {
                    is Winner.LocalPlayer -> theme.textHighlight
                    is Winner.Opponent -> Color.Red
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your Score: ${localStats.score}",
                    fontSize = 18.sp,
                    color = theme.textPrimary
                )
                Text(
                    text = "Level: ${localStats.level}",
                    fontSize = 16.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = "Lines: ${localStats.linesCleared}",
                    fontSize = 16.sp,
                    color = theme.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBackToMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.textHighlight,
                    contentColor = theme.background
                )
            ) {
                Text("BACK TO MENU", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = theme.background,
        textContentColor = theme.textPrimary
    )
}
