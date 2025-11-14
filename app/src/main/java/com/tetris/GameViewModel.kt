package com.tetris

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tetris.data.GamePreferences
import com.tetris.game.GameState
import com.tetris.game.GameStats
import com.tetris.game.Tetromino
import com.tetris.game.TetrisGame
import com.tetris.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

/**
 * ViewModel for the Tetris game
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GamePreferences(application)

    // Current theme
    private val _currentTheme = MutableStateFlow(MinimalisticTheme)
    val currentTheme: StateFlow<TetrisTheme> = _currentTheme.asStateFlow()

    // High score
    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Screen state
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Menu)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Game instance
    private var game: TetrisGame? = null

    // Game state flows
    val gameState: StateFlow<GameState> = MutableStateFlow(GameState.Menu)
    val stats: StateFlow<GameStats> = MutableStateFlow(GameStats())
    val currentPiece: StateFlow<Tetromino?> = MutableStateFlow(null)
    val nextPiece: StateFlow<Tetromino?> = MutableStateFlow(null)
    val boardState: StateFlow<List<List<Color?>>> = MutableStateFlow(emptyList())
    val lineClearAnimation: StateFlow<Set<Int>> = MutableStateFlow(emptySet())

    init {
        // Load preferences
        viewModelScope.launch {
            preferences.theme.collect { themeName ->
                _currentTheme.value = AllThemes.find { it.name == themeName } ?: MinimalisticTheme
            }
        }

        viewModelScope.launch {
            preferences.highScore.collect { score ->
                _highScore.value = score
            }
        }
    }

    /**
     * Start a new game
     */
    fun startGame() {
        val theme = _currentTheme.value
        game?.dispose()
        game = TetrisGame(theme.shapeColors)

        // Connect game flows to ViewModel flows
        viewModelScope.launch {
            game?.gameState?.collect { state ->
                (gameState as MutableStateFlow).value = state

                // Save high score when game is over
                if (state is GameState.GameOver) {
                    preferences.saveHighScore(state.score)
                }
            }
        }

        viewModelScope.launch {
            game?.stats?.collect { (stats as MutableStateFlow).value = it }
        }

        viewModelScope.launch {
            game?.currentPiece?.collect { (currentPiece as MutableStateFlow).value = it }
        }

        viewModelScope.launch {
            game?.nextPiece?.collect { (nextPiece as MutableStateFlow).value = it }
        }

        viewModelScope.launch {
            game?.boardState?.collect { (boardState as MutableStateFlow).value = it }
        }

        viewModelScope.launch {
            game?.lineClearAnimation?.collect { (lineClearAnimation as MutableStateFlow).value = it }
        }

        game?.startGame()
        _screenState.value = ScreenState.Game
    }

    /**
     * Pause the game
     */
    fun pauseGame() {
        game?.pauseGame()
    }

    /**
     * Resume the game
     */
    fun resumeGame() {
        game?.resumeGame()
    }

    /**
     * Move piece left
     */
    fun moveLeft() {
        game?.moveLeft()
    }

    /**
     * Move piece right
     */
    fun moveRight() {
        game?.moveRight()
    }

    /**
     * Move piece down
     */
    fun moveDown() {
        game?.moveDown()
    }

    /**
     * Rotate piece
     */
    fun rotatePiece() {
        game?.rotatePiece()
    }

    /**
     * Hard drop
     */
    fun hardDrop() {
        game?.hardDrop()
    }

    /**
     * Return to menu
     */
    fun returnToMenu() {
        game?.returnToMenu()
        _screenState.value = ScreenState.Menu
    }

    override fun onCleared() {
        super.onCleared()
        game?.dispose()
    }
}

/**
 * Screen states
 */
sealed class ScreenState {
    object Menu : ScreenState()
    object Game : ScreenState()
}
