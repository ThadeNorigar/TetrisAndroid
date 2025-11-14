package com.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tetris.game.GameState
import com.tetris.ui.GameScreen
import com.tetris.ui.MenuScreen
import com.tetris.ui.LobbyScreen
import com.tetris.ui.LobbyViewModel
import com.tetris.ui.MultiplayerGameScreen

/**
 * Main activity for the Tetris game
 */
class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TetrisApp(viewModel)
        }
    }
}

@Composable
fun TetrisApp(viewModel: GameViewModel) {
    val screenState by viewModel.screenState.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val highScore by viewModel.highScore.collectAsState()

    when (screenState) {
        is ScreenState.Menu -> {
            MenuScreen(
                theme = currentTheme,
                highScore = highScore,
                onStartGame = { viewModel.startGame() },
                onVsPlayer = { viewModel.navigateToLobby() },
                modifier = Modifier.fillMaxSize()
            )
        }

        is ScreenState.Lobby -> {
            LobbyScreen(
                theme = currentTheme,
                onBack = { viewModel.returnToMenu() },
                onGameStart = { viewModel.navigateToMultiplayerGame() },
                modifier = Modifier.fillMaxSize()
            )
        }

        is ScreenState.MultiplayerGame -> {
            // Get NetworkManager from LobbyViewModel
            val networkManager = LobbyViewModel.sharedNetworkManager
            if (networkManager != null) {
                MultiplayerGameScreen(
                    theme = currentTheme,
                    networkManager = networkManager,
                    onBackToMenu = {
                        // Clean up
                        LobbyViewModel.sharedNetworkManager = null
                        viewModel.returnToMenu()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback if no network manager (shouldn't happen)
                viewModel.returnToMenu()
            }
        }

        is ScreenState.Game -> {
            val gameState by viewModel.gameState.collectAsState()
            val stats by viewModel.stats.collectAsState()
            val board by viewModel.boardState.collectAsState()
            val currentPiece by viewModel.currentPiece.collectAsState()
            val nextPiece by viewModel.nextPiece.collectAsState()
            val lineClearAnimation by viewModel.lineClearAnimation.collectAsState()

            GameScreen(
                gameState = gameState,
                stats = stats,
                board = board,
                currentPiece = currentPiece,
                nextPiece = nextPiece,
                lineClearAnimation = lineClearAnimation,
                theme = currentTheme,
                highScore = highScore,
                onMoveLeft = { viewModel.moveLeft() },
                onMoveRight = { viewModel.moveRight() },
                onMoveDown = { viewModel.moveDown() },
                onRotate = { viewModel.rotatePiece() },
                onHardDrop = { viewModel.hardDrop() },
                onPause = { viewModel.pauseGame() },
                onResume = { viewModel.resumeGame() },
                onBackToMenu = { viewModel.returnToMenu() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
