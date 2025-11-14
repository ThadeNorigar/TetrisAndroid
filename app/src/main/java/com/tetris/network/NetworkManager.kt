package com.tetris.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.InetAddress

/**
 * Manages network connections for multiplayer game
 */
class NetworkManager(private val context: Context) {
    private val tag = "NetworkManager"
    private val serviceName = "TetrisGame"
    private val serviceType = "_tetris._tcp"
    private val port = 8888

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Reconnection support
    private var lastConnectedPlayer: PlayerInfo? = null
    private var isHost: Boolean = false
    private var hostPlayerName: String? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var keepAliveJob: Job? = null
    private var reconnectJob: Job? = null

    // State flows
    private val _discoveredPlayers = MutableStateFlow<List<PlayerInfo>>(emptyList())
    val discoveredPlayers: StateFlow<List<PlayerInfo>> = _discoveredPlayers

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _receivedMessages = MutableStateFlow<GameMessage?>(null)
    val receivedMessages: StateFlow<GameMessage?> = _receivedMessages

    /**
     * Start hosting a game
     */
    suspend fun startHosting(playerName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Start server socket
            val selectorManager = SelectorManager(Dispatchers.IO)
            serverSocket = aSocket(selectorManager).tcp().bind(port = port)

            Log.d(tag, "Server socket bound to port $port")

            // Register NSD service
            registerService(playerName)

            isHost = true
            hostPlayerName = playerName
            _connectionState.value = ConnectionState.Hosting(playerName)

            // Wait for client connection
            scope.launch {
                try {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        clientSocket = socket
                        reconnectAttempts = 0
                        _connectionState.value = ConnectionState.Connected
                        startKeepAlive()
                        startReceivingMessages(socket)
                        Log.d(tag, "Client connected")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error accepting connection", e)
                    _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error starting host", e)
            Result.failure(e)
        }
    }

    /**
     * Start discovering available games
     */
    fun startDiscovery() {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(tag, "Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == serviceType) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(tag, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.d(tag, "Service resolved: ${serviceInfo.serviceName}")
                            val playerInfo = PlayerInfo(
                                name = serviceInfo.serviceName,
                                address = serviceInfo.host,
                                port = serviceInfo.port
                            )
                            val currentList = _discoveredPlayers.value.toMutableList()
                            if (currentList.none { it.name == playerInfo.name }) {
                                currentList.add(playerInfo)
                                _discoveredPlayers.value = currentList
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service lost: ${serviceInfo.serviceName}")
                _discoveredPlayers.value = _discoveredPlayers.value.filter {
                    it.name != serviceInfo.serviceName
                }
            }
        }

        this.discoveryListener = discoveryListener
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping discovery", e)
            }
        }
        discoveryListener = null
        _discoveredPlayers.value = emptyList()
    }

    /**
     * Connect to a host
     */
    suspend fun connectToHost(playerInfo: PlayerInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting

            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(
                remoteAddress = InetSocketAddress(playerInfo.address.hostAddress ?: playerInfo.address.hostName, playerInfo.port)
            )

            clientSocket = socket
            lastConnectedPlayer = playerInfo
            isHost = false
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Connected
            startKeepAlive()
            startReceivingMessages(socket)

            Log.d(tag, "Connected to host ${playerInfo.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error connecting to host", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    /**
     * Send a message to the connected peer
     */
    suspend fun sendMessage(message: GameMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val socket = clientSocket ?: return@withContext Result.failure(
                Exception("Not connected")
            )

            val jsonString = json.encodeToString(message)
            val messageWithDelimiter = "$jsonString\n"

            socket.openWriteChannel(autoFlush = true).writeStringUtf8(messageWithDelimiter)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Start receiving messages from socket
     */
    private fun startReceivingMessages(socket: Socket) {
        scope.launch {
            try {
                val receiveChannel = socket.openReadChannel()

                while (isActive) {
                    try {
                        val line = receiveChannel.readUTF8Line() ?: break
                        if (line.isNotEmpty()) {
                            val message = json.decodeFromString<GameMessage>(line)
                            _receivedMessages.value = message
                            Log.d(tag, "Received message: $message")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error reading message", e)
                        break
                    }
                }

                // Connection lost - attempt reconnection
                keepAliveJob?.cancel()
                handleDisconnection()
            } catch (e: Exception) {
                Log.e(tag, "Error in receive loop", e)
                keepAliveJob?.cancel()
                handleDisconnection()
            }
        }
    }

    /**
     * Handle disconnection and attempt reconnect
     */
    private fun handleDisconnection() {
        scope.launch {
            Log.d(tag, "Connection lost, attempting to reconnect...")
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempts + 1)

            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                delay(2000L * reconnectAttempts) // Exponential backoff

                val reconnectSuccess = if (isHost) {
                    attemptReconnectAsHost()
                } else {
                    attemptReconnectAsClient()
                }

                if (!reconnectSuccess && reconnectAttempts < maxReconnectAttempts) {
                    // Try again
                    handleDisconnection()
                } else if (!reconnectSuccess) {
                    // Max attempts reached
                    _connectionState.value = ConnectionState.Disconnected
                    _receivedMessages.value = GameMessage.PlayerDisconnected
                    Log.e(tag, "Reconnection failed after $maxReconnectAttempts attempts")
                }
            } else {
                _connectionState.value = ConnectionState.Disconnected
                _receivedMessages.value = GameMessage.PlayerDisconnected
            }
        }
    }

    /**
     * Attempt reconnection as host (wait for client to reconnect)
     */
    private suspend fun attemptReconnectAsHost(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Waiting for client to reconnect (attempt $reconnectAttempts)...")

            // Close old socket
            clientSocket?.close()
            clientSocket = null

            // Wait for new connection with timeout
            withTimeout(10000L) {
                val socket = serverSocket?.accept()
                if (socket != null) {
                    clientSocket = socket
                    reconnectAttempts = 0
                    _connectionState.value = ConnectionState.Connected
                    startKeepAlive()
                    startReceivingMessages(socket)
                    Log.d(tag, "Client reconnected successfully")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(tag, "Reconnect as host failed", e)
            false
        }
    }

    /**
     * Attempt reconnection as client
     */
    private suspend fun attemptReconnectAsClient(): Boolean = withContext(Dispatchers.IO) {
        try {
            val playerInfo = lastConnectedPlayer ?: return@withContext false
            Log.d(tag, "Attempting to reconnect to ${playerInfo.name} (attempt $reconnectAttempts)...")

            // Close old socket
            clientSocket?.close()
            clientSocket = null

            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(
                remoteAddress = InetSocketAddress(playerInfo.address.hostAddress ?: playerInfo.address.hostName, playerInfo.port)
            )

            clientSocket = socket
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Connected
            startKeepAlive()
            startReceivingMessages(socket)
            Log.d(tag, "Reconnected successfully to ${playerInfo.name}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Reconnect as client failed", e)
            false
        }
    }

    /**
     * Start keep-alive ping mechanism
     */
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(5000L) // Send ping every 5 seconds
                try {
                    sendMessage(GameMessage.Ping(System.currentTimeMillis()))
                } catch (e: Exception) {
                    Log.e(tag, "Keep-alive ping failed", e)
                    break
                }
            }
        }
    }

    /**
     * Register NSD service
     */
    private fun registerService(playerName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = playerName
            serviceType = this@NetworkManager.serviceType
            port = this@NetworkManager.port
        }

        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service registration failed: $errorCode")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service unregistration failed: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service registered: ${serviceInfo.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service unregistered")
            }
        }

        this.registrationListener = registrationListener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    /**
     * Unregister NSD service
     */
    private fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(tag, "Error unregistering service", e)
            }
        }
        registrationListener = null
    }

    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        scope.launch {
            keepAliveJob?.cancel()
            reconnectJob?.cancel()

            try {
                clientSocket?.close()
                serverSocket?.close()
            } catch (e: Exception) {
                Log.e(tag, "Error closing sockets", e)
            }

            clientSocket = null
            serverSocket = null
            lastConnectedPlayer = null
            reconnectAttempts = 0

            stopDiscovery()
            unregisterService()

            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Represents a discovered player
 */
data class PlayerInfo(
    val name: String,
    val address: InetAddress,
    val port: Int
)

/**
 * Connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Hosting(val playerName: String) : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Reconnecting(val attempt: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
