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
                modifier = Modifier.fillMaxSize()
            )
        }

        is ScreenState.Game -> {
            val gameState by viewModel.gameState.collectAsState()
            val stats by viewModel.stats.collectAsState()
            val board by viewModel.boardState.collectAsState()
            val currentPiece by viewModel.currentPiece.collectAsState()
            val nextPiece by viewModel.nextPiece.collectAsState()

            GameScreen(
                gameState = gameState,
                stats = stats,
                board = board,
                currentPiece = currentPiece,
                nextPiece = nextPiece,
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
