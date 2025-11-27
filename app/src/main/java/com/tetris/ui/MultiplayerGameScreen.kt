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
import com.tetris.network.ConnectionState
import com.tetris.network.NetworkManager
import com.tetris.ui.theme.TetrisTheme
import com.tetris.ui.components.GameBoard
import com.tetris.ui.components.GameControls
import com.tetris.ui.components.NextPiecePreview
import androidx.compose.material3.CircularProgressIndicator

/**
 * Multiplayer game screen showing both players' boards side by side
 */
@Composable
fun MultiplayerGameScreen(
    theme: TetrisTheme,
    networkManager: NetworkManager,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create ViewModel with NetworkManager
    val viewModel: MultiplayerGameViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MultiplayerGameViewModel(
                    context.applicationContext as Application,
                    networkManager,
                    theme
                ) as T
            }
        }
    )

    // Collect game states
    val localGameState by viewModel.localGame.gameState.collectAsState()
    val localStats by viewModel.localGame.stats.collectAsState()
    val localBoard by viewModel.localGame.boardState.collectAsState()
    val localCurrentPiece by viewModel.localGame.currentPiece.collectAsState()
    val localLineClearAnimation by viewModel.localGame.lineClearAnimation.collectAsState()

    val opponentStats by viewModel.opponentStats.collectAsState()
    val opponentBoard by viewModel.opponentBoardState.collectAsState()
    val opponentCurrentPiece by viewModel.opponentCurrentPiece.collectAsState()
    val opponentNextPiece by viewModel.opponentNextPiece.collectAsState()

    val localNextPiece by viewModel.localGame.nextPiece.collectAsState()

    val winner by viewModel.winner.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Show winner dialog
    if (winner != null) {
        MultiplayerGameOverDialog(
            winner = winner!!,
            localStats = localStats,
            theme = theme,
            onPlayAgain = {
                viewModel.restartGame()
            },
            onBackToMenu = {
                viewModel.cleanup()
                onBackToMenu()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title
            Text(
                text = "VS PLAYER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textHighlight,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            // Boards Row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Local Player (Left)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textHighlight,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Stats
                    MiniStatsDisplay(
                        stats = localStats,
                        theme = theme,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Next Piece Preview
                    NextPiecePreview(
                        nextPiece = localNextPiece,
                        theme = theme,
                        useGraphics = true,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Board
                    GameBoard(
                        board = localBoard,
                        currentPiece = localCurrentPiece,
                        lineClearAnimation = localLineClearAnimation,
                        theme = theme,
                        useGraphics = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.5f)
                    )
                }

                // Opponent Player (Right)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OPPONENT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Stats
                    MiniStatsDisplay(
                        stats = opponentStats,
                        theme = theme,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Next Piece Preview
                    NextPiecePreview(
                        nextPiece = opponentNextPiece,
                        theme = theme,
                        useGraphics = true,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Board - now showing opponent's current piece in real-time
                    GameBoard(
                        board = opponentBoard,
                        currentPiece = opponentCurrentPiece,
                        lineClearAnimation = emptySet(),
                        theme = theme,
                        useGraphics = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.5f)
                    )
                }
            }

            // Controls
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Game controls
                if (localGameState is GameState.Playing) {
                    GameControls(
                        theme = theme,
                        onMoveLeft = { viewModel.moveLeft() },
                        onMoveRight = { viewModel.moveRight() },
                        onMoveDown = { viewModel.moveDown() },
                        onRotate = { viewModel.rotatePiece() },
                        onHardDrop = { viewModel.hardDrop() },
                        useGraphics = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menu buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (localGameState is GameState.Playing) {
                        Button(
                            onClick = { viewModel.pauseGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.gridBorder,
                                contentColor = theme.textPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PAUSE", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (localGameState is GameState.Paused) {
                        Button(
                            onClick = { viewModel.resumeGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.textHighlight,
                                contentColor = theme.background
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESUME", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.cleanup()
                            onBackToMenu()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.gridBorder,
                            contentColor = theme.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("QUIT", fontSize = 14.sp)
                    }
                }
            }
        }

        // Reconnection overlay
        if (connectionState is ConnectionState.Reconnecting) {
            ReconnectionOverlay(
                theme = theme,
                attempt = (connectionState as ConnectionState.Reconnecting).attempt,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Overlay shown during reconnection attempts
 */
@Composable
private fun ReconnectionOverlay(
    theme: TetrisTheme,
    attempt: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(theme.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = theme.textHighlight,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CONNECTION LOST",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textHighlight
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reconnecting... (Attempt $attempt/5)",
                fontSize = 16.sp,
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please wait",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
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
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = when (winner) {
                    is Winner.LocalPlayer -> "YOU WIN!"
                    is Winner.Opponent -> "YOU LOSE"
                    is Winner.Disconnected -> "CONNECTION LOST"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = when (winner) {
                    is Winner.LocalPlayer -> theme.textHighlight
                    is Winner.Opponent -> Color.Red
                    is Winner.Disconnected -> Color(0xFFFF9800) // Orange
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (winner is Winner.Disconnected) {
                    Text(
                        text = "The connection to your opponent was lost.",
                        fontSize = 16.sp,
                        color = theme.textPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
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
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Only show PLAY AGAIN if not disconnected
                if (winner !is Winner.Disconnected) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.textHighlight,
                            contentColor = theme.background
                        )
                    ) {
                        Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (winner is Winner.Disconnected) theme.textHighlight else theme.gridBorder,
                        contentColor = if (winner is Winner.Disconnected) theme.background else theme.textPrimary
                    )
                ) {
                    Text("BACK TO MENU", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = theme.background,
        textContentColor = theme.textPrimary
    )
}
