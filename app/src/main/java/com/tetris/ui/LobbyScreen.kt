package com.tetris.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetris.network.ConnectionState
import com.tetris.network.NetworkManager
import com.tetris.network.PlayerInfo
import com.tetris.ui.theme.TetrisTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Lobby screen
 */
class LobbyViewModel(application: Application) : AndroidViewModel(application) {
    private val networkManager = NetworkManager(application)

    val discoveredPlayers: StateFlow<List<PlayerInfo>> = networkManager.discoveredPlayers
    val connectionState: StateFlow<ConnectionState> = networkManager.connectionState

    private val _playerName = MutableStateFlow("Player")
    val playerName: StateFlow<String> = _playerName

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    fun setPlayerName(name: String) {
        _playerName.value = name
    }

    fun startHosting() {
        _isHost.value = true
        viewModelScope.launch {
            networkManager.startHosting(_playerName.value)
        }
    }

    fun startDiscovery() {
        _isHost.value = false
        networkManager.startDiscovery()
    }

    fun connectToPlayer(playerInfo: PlayerInfo) {
        viewModelScope.launch {
            networkManager.connectToHost(playerInfo)
        }
    }

    fun cancelAndGoBack() {
        networkManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        networkManager.cleanup()
    }

    fun getNetworkManager(): NetworkManager = networkManager
}

/**
 * Lobby screen for multiplayer matchmaking
 */
@Composable
fun LobbyScreen(
    theme: TetrisTheme,
    onBack: () -> Unit,
    onGameStart: (NetworkManager) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LobbyViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LobbyViewModel(context.applicationContext as Application) as T
            }
        }
    )

    val playerName by viewModel.playerName.collectAsState()
    val discoveredPlayers by viewModel.discoveredPlayers.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isHost by viewModel.isHost.collectAsState()

    var showNameInput by remember { mutableStateOf(true) }
    var showModeSelection by remember { mutableStateOf(false) }

    // Handle connection state changes
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                // Both players connected, pass NetworkManager and start game
                onGameStart(viewModel.getNetworkManager())
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelAndGoBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(32.dp)
    ) {
        when {
            showNameInput -> {
                NameInputScreen(
                    theme = theme,
                    playerName = playerName,
                    onNameChange = { viewModel.setPlayerName(it) },
                    onContinue = {
                        showNameInput = false
                        showModeSelection = true
                    },
                    onBack = onBack
                )
            }

            showModeSelection -> {
                ModeSelectionScreen(
                    theme = theme,
                    onHostGame = {
                        viewModel.startHosting()
                        showModeSelection = false
                    },
                    onJoinGame = {
                        viewModel.startDiscovery()
                        showModeSelection = false
                    },
                    onBack = {
                        showModeSelection = false
                        showNameInput = true
                    }
                )
            }

            isHost && connectionState is ConnectionState.Hosting -> {
                HostingScreen(
                    theme = theme,
                    playerName = playerName,
                    onBack = {
                        viewModel.cancelAndGoBack()
                        onBack()
                    }
                )
            }

            !isHost -> {
                JoinGameScreen(
                    theme = theme,
                    discoveredPlayers = discoveredPlayers,
                    connectionState = connectionState,
                    onPlayerSelected = { viewModel.connectToPlayer(it) },
                    onBack = {
                        viewModel.cancelAndGoBack()
                        showModeSelection = true
                    }
                )
            }
        }
    }
}

@Composable
private fun NameInputScreen(
    theme: TetrisTheme,
    playerName: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ENTER YOUR NAME",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight
        )

        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = playerName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.gridBorder.copy(alpha = 0.3f),
                unfocusedContainerColor = theme.gridBorder.copy(alpha = 0.2f),
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary
            ),
            singleLine = true,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.textHighlight,
                contentColor = theme.background
            )
        ) {
            Text("CONTINUE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.gridBorder,
                contentColor = theme.textPrimary
            )
        ) {
            Text("BACK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModeSelectionScreen(
    theme: TetrisTheme,
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MULTIPLAYER MODE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onHostGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.textHighlight,
                contentColor = theme.background
            )
        ) {
            Text("HOST GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onJoinGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.textHighlight,
                contentColor = theme.background
            )
        ) {
            Text("JOIN GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.gridBorder,
                contentColor = theme.textPrimary
            )
        ) {
            Text("BACK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HostingScreen(
    theme: TetrisTheme,
    playerName: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HOSTING GAME",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Host: $playerName",
            fontSize = 18.sp,
            color = theme.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(color = theme.textHighlight)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Waiting for opponent...",
            fontSize = 16.sp,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.gridBorder,
                contentColor = theme.textPrimary
            )
        ) {
            Text("CANCEL", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JoinGameScreen(
    theme: TetrisTheme,
    discoveredPlayers: List<PlayerInfo>,
    connectionState: ConnectionState,
    onPlayerSelected: (PlayerInfo) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AVAILABLE GAMES",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textHighlight,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when (connectionState) {
            is ConnectionState.Connecting -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = theme.textHighlight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting...",
                            fontSize = 16.sp,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            is ConnectionState.Error -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Connection Failed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textHighlight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connectionState.message,
                            fontSize = 14.sp,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            else -> {
                if (discoveredPlayers.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = theme.textHighlight)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching for games...",
                                fontSize = 16.sp,
                                color = theme.textSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(discoveredPlayers) { player ->
                            PlayerCard(
                                playerInfo = player,
                                theme = theme,
                                onClick = { onPlayerSelected(player) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.gridBorder,
                contentColor = theme.textPrimary
            )
        ) {
            Text("BACK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlayerCard(
    playerInfo: PlayerInfo,
    theme: TetrisTheme,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.gridBorder.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = playerInfo.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "IP: ${playerInfo.address.hostAddress}",
                fontSize = 14.sp,
                color = theme.textSecondary
            )
        }
    }
}
