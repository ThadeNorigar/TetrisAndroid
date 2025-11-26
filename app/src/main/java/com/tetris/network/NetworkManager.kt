package com.tetris.network

import android.content.Context
import android.net.wifi.WifiManager
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
    private val serviceType = "_tetris._tcp."
    private val port = 8888

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var selectorManager: SelectorManager? = null
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private val writeLock = Any()  // Synchronize write operations
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var receiveJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // Service resolution queue to handle one resolve at a time
    private val resolveQueue = mutableListOf<NsdServiceInfo>()
    private var isResolving = false

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
        Log.d(tag, "=== startHosting() CALLED ===")
        Log.d(tag, "Player name: $playerName")
        Log.d(tag, "Thread: ${Thread.currentThread().name}")

        try {
            // Step 1: Acquire multicast lock for NSD to work properly
            Log.d(tag, "STEP 1: Acquiring multicast lock...")
            try {
                acquireMulticastLock()
                Log.d(tag, "✓ STEP 1 completed: Multicast lock acquired")
            } catch (e: Exception) {
                Log.e(tag, "✗ STEP 1 FAILED: Error acquiring multicast lock", e)
                throw e
            }

            // Step 2: Initialize SelectorManager if not already created
            Log.d(tag, "STEP 2: Initializing SelectorManager...")
            try {
                if (selectorManager == null) {
                    selectorManager = SelectorManager(Dispatchers.IO)
                    Log.d(tag, "✓ STEP 2 completed: SelectorManager created")
                } else {
                    Log.d(tag, "✓ STEP 2 skipped: SelectorManager already exists")
                }
            } catch (e: Exception) {
                Log.e(tag, "✗ STEP 2 FAILED: Error creating SelectorManager", e)
                throw e
            }

            // Step 3: Start server socket
            Log.d(tag, "STEP 3: Binding server socket to port $port...")
            try {
                serverSocket = aSocket(selectorManager!!).tcp().bind(port = port)
                Log.d(tag, "✓ STEP 3 completed: Server socket bound to port $port")
                Log.d(tag, "Server socket details: ${serverSocket?.localAddress}")
            } catch (e: Exception) {
                Log.e(tag, "✗ STEP 3 FAILED: Error binding server socket", e)
                Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
                Log.e(tag, "Exception message: ${e.message}")
                throw e
            }

            // Step 4: Register NSD service
            Log.d(tag, "STEP 4: Registering NSD service...")
            try {
                registerService(playerName)
                Log.d(tag, "✓ STEP 4 completed: NSD service registration initiated")
            } catch (e: Exception) {
                Log.e(tag, "✗ STEP 4 FAILED: Error registering NSD service", e)
                throw e
            }

            // Step 5: Update state
            Log.d(tag, "STEP 5: Updating connection state...")
            isHost = true
            hostPlayerName = playerName
            _connectionState.value = ConnectionState.Hosting(playerName)
            Log.d(tag, "✓ STEP 5 completed: State updated to Hosting")

            // Step 6: Wait for client connection
            Log.d(tag, "STEP 6: Launching coroutine to accept client connections...")
            scope.launch {
                try {
                    Log.d(tag, "Waiting for client to connect...")
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        Log.d(tag, "✓ Client connected from: ${socket.remoteAddress}")
                        clientSocket = socket
                        writeChannel = socket.openWriteChannel(autoFlush = true)
                        reconnectAttempts = 0
                        _connectionState.value = ConnectionState.Connected
                        Log.d(tag, "✓ Write channel established")
                        startKeepAlive()
                        startReceivingMessages(socket)
                        Log.d(tag, "✓ Client connection fully established")
                    } else {
                        Log.e(tag, "✗ serverSocket.accept() returned null")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "✗ Error accepting connection", e)
                    Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
                    Log.e(tag, "Exception message: ${e.message}")
                    Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
                    _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                }
            }
            Log.d(tag, "✓ STEP 6 completed: Accept coroutine launched")

            Log.d(tag, "=== startHosting() COMPLETED SUCCESSFULLY ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "=== startHosting() FAILED ===")
            Log.e(tag, "Error starting host", e)
            Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
            Log.e(tag, "Exception message: ${e.message}")
            Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
            _connectionState.value = ConnectionState.Error(e.message ?: "Failed to start hosting")

            // Cleanup on failure
            try {
                serverSocket?.close()
                serverSocket = null
                unregisterService()
                releaseMulticastLock()
            } catch (cleanupException: Exception) {
                Log.e(tag, "Error during cleanup after failure", cleanupException)
            }

            return@withContext Result.failure(e)
        }
    }

    /**
     * Start discovering available games
     */
    fun startDiscovery() {
        Log.d(tag, "=== startDiscovery() called ===")
        Log.d(tag, "Service type to discover: $serviceType")

        // Acquire multicast lock for NSD to work properly
        acquireMulticastLock()

        try {
            val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "✗ Discovery start FAILED: errorCode=$errorCode, serviceType=$serviceType")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "✗ Discovery stop FAILED: errorCode=$errorCode, serviceType=$serviceType")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(tag, "✓ Discovery STARTED: serviceType=$serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "✓ Discovery STOPPED: serviceType=$serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service found: ${serviceInfo.serviceName}, type: ${serviceInfo.serviceType}")
                // Use startsWith because Android may append domain info like ".local."
                if (serviceInfo.serviceType.startsWith("_tetris._tcp")) {
                    Log.d(tag, "Service type matches, attempting to resolve...")
                    resolveService(serviceInfo)
                } else {
                    Log.d(tag, "Service type doesn't match, ignoring")
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
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Failed to start discovery")
        }
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

        // Clear resolve queue
        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }
    }

    /**
     * Connect to a host
     */
    suspend fun connectToHost(playerInfo: PlayerInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting

            // Initialize SelectorManager if not already created
            if (selectorManager == null) {
                selectorManager = SelectorManager(Dispatchers.IO)
            }

            val socket = aSocket(selectorManager!!).tcp().connect(
                remoteAddress = InetSocketAddress(playerInfo.address.hostAddress ?: playerInfo.address.hostName, playerInfo.port)
            )

            clientSocket = socket
            writeChannel = socket.openWriteChannel(autoFlush = true)
            lastConnectedPlayer = playerInfo
            isHost = false
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Connected
            Log.d(tag, "Write channel established")
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
            val channel = writeChannel ?: return@withContext Result.failure(
                Exception("Not connected - write channel not initialized")
            )

            val jsonString = json.encodeToString(message)
            val messageWithDelimiter = "$jsonString\n"

            // Synchronize writes to prevent concurrent access issues
            synchronized(writeLock) {
                channel.writeStringUtf8(messageWithDelimiter)
                channel.flush()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error sending message: ${e.message}", e)
            // Connection likely broken, trigger disconnection handling
            Result.failure(e)
        }
    }

    /**
     * Start receiving messages from socket
     */
    private fun startReceivingMessages(socket: Socket) {
        // Cancel previous receive job if exists
        receiveJob?.cancel()

        receiveJob = scope.launch {
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

            // Close old socket and channel
            writeChannel = null
            clientSocket?.close()
            clientSocket = null

            // Wait for new connection with timeout
            val socket = withTimeoutOrNull(10000L) {
                serverSocket?.accept()
            }

            if (socket != null) {
                clientSocket = socket
                writeChannel = socket.openWriteChannel(autoFlush = true)
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.Connected
                Log.d(tag, "Write channel re-established")
                startKeepAlive()
                startReceivingMessages(socket)
                Log.d(tag, "Client reconnected successfully")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Reconnect as host failed", e)
            false
        }
    }

    /**
     * Attempt reconnection as client
     */
    private suspend fun attemptReconnectAsClient(): Boolean = withContext(Dispatchers.IO) {
        val playerInfo = lastConnectedPlayer ?: return@withContext false

        try {
            Log.d(tag, "Attempting to reconnect to ${playerInfo.name} (attempt $reconnectAttempts)...")

            // Close old socket and channel
            writeChannel = null
            clientSocket?.close()
            clientSocket = null

            // Initialize SelectorManager if not already created
            if (selectorManager == null) {
                selectorManager = SelectorManager(Dispatchers.IO)
            }

            val socket = aSocket(selectorManager!!).tcp().connect(
                remoteAddress = InetSocketAddress(playerInfo.address.hostAddress ?: playerInfo.address.hostName, playerInfo.port)
            )

            clientSocket = socket
            writeChannel = socket.openWriteChannel(autoFlush = true)
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Connected
            Log.d(tag, "Write channel re-established")
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
        Log.d(tag, "=== registerService() called ===")
        Log.d(tag, "Player name: $playerName")
        Log.d(tag, "Service type: $serviceType")
        Log.d(tag, "Port: $port")

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = playerName
            serviceType = this@NetworkManager.serviceType
            port = this@NetworkManager.port
        }

        Log.d(tag, "ServiceInfo created: name=${serviceInfo.serviceName}, type=${serviceInfo.serviceType}, port=${serviceInfo.port}")

        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "✗ Service registration FAILED: errorCode=$errorCode")
                Log.e(tag, "Failed service name: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "✗ Service unregistration FAILED: errorCode=$errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // IMPORTANT: Android NSD may return null values in the callback parameter.
                // We use the original values from our class instead of the callback parameter.
                Log.d(tag, "✓ Service REGISTERED successfully")
                Log.d(tag, "  Registered name: ${serviceInfo.serviceName}")
                Log.d(tag, "  Expected type: ${this@NetworkManager.serviceType}")
                Log.d(tag, "  Expected port: ${this@NetworkManager.port}")
                // Note: serviceInfo.serviceType and serviceInfo.port may be null here
                // This is normal Android NSD behavior - the system may modify these values
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "✓ Service UNREGISTERED: ${serviceInfo.serviceName}")
            }
        }

        this.registrationListener = registrationListener
        try {
            Log.d(tag, "Calling nsdManager.registerService()...")
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            Log.d(tag, "✓ nsdManager.registerService() call completed")
        } catch (e: Exception) {
            Log.e(tag, "✗ Exception in registerService", e)
            throw e
        }
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
     * Resolve service with queue to handle one at a time
     */
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        synchronized(resolveQueue) {
            resolveQueue.add(serviceInfo)
            if (!isResolving) {
                processNextResolve()
            }
        }
    }

    /**
     * Process next service in resolve queue
     */
    private fun processNextResolve() {
        synchronized(resolveQueue) {
            if (resolveQueue.isEmpty()) {
                isResolving = false
                return
            }

            isResolving = true
            val serviceInfo = resolveQueue.removeAt(0)

            try {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(tag, "Resolve failed for ${serviceInfo.serviceName}: errorCode=$errorCode")
                        // Process next in queue
                        processNextResolve()
                    }

                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        Log.d(tag, "Service resolved: ${resolvedService.serviceName} at ${resolvedService.host}:${resolvedService.port}")
                        val playerInfo = PlayerInfo(
                            name = resolvedService.serviceName,
                            address = resolvedService.host,
                            port = resolvedService.port
                        )
                        val currentList = _discoveredPlayers.value.toMutableList()
                        if (currentList.none { it.name == playerInfo.name }) {
                            currentList.add(playerInfo)
                            _discoveredPlayers.value = currentList
                            Log.d(tag, "Added player to list: ${playerInfo.name}")
                        }
                        // Process next in queue
                        processNextResolve()
                    }
                })
            } catch (e: Exception) {
                Log.e(tag, "Exception during resolve", e)
                processNextResolve()
            }
        }
    }

    /**
     * Acquire WiFi multicast lock for NSD to work
     */
    private fun acquireMulticastLock() {
        Log.d(tag, "acquireMulticastLock() called")
        Log.d(tag, "Current multicastLock: ${if (multicastLock == null) "null" else "exists"}")
        Log.d(tag, "Is held: ${multicastLock?.isHeld ?: false}")

        if (multicastLock == null || !multicastLock!!.isHeld) {
            try {
                Log.d(tag, "Getting WifiManager service...")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                Log.d(tag, "✓ WifiManager obtained")
                Log.d(tag, "WiFi enabled: ${wifiManager.isWifiEnabled}")

                Log.d(tag, "Creating multicast lock...")
                multicastLock = wifiManager.createMulticastLock("TetrisMulticastLock").apply {
                    setReferenceCounted(true)
                    Log.d(tag, "Acquiring multicast lock...")
                    acquire()
                }
                Log.d(tag, "✓ Multicast lock acquired successfully")
            } catch (e: Exception) {
                Log.e(tag, "✗ Failed to acquire multicast lock", e)
                Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
                Log.e(tag, "Exception message: ${e.message}")
                throw e
            }
        } else {
            Log.d(tag, "✓ Multicast lock already held, skipping")
        }
    }

    /**
     * Release WiFi multicast lock
     */
    private fun releaseMulticastLock() {
        multicastLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                    Log.d(tag, "✓ Multicast lock released")
                } catch (e: Exception) {
                    Log.e(tag, "Error releasing multicast lock", e)
                }
            }
        }
        multicastLock = null
    }

    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        scope.launch {
            keepAliveJob?.cancel()
            reconnectJob?.cancel()
            receiveJob?.cancel()

            try {
                writeChannel = null
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
            releaseMulticastLock()

            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()

        // Close SelectorManager to free native resources
        selectorManager?.close()
        selectorManager = null

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
