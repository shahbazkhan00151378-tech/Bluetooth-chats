package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BleMeshManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var activeClientGatt: BluetoothGatt? = null
    private var connectingDeviceAddress: String? = null

    // Handlers on Main Looper
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val scanTimeoutRunnable = Runnable {
        if (_isScanning.value) {
            val peerCount = _discoveredPeers.value.size
            _statusLog.value = "Scan finished (timed out after 20s). Discovered $peerCount device(s)."
            stopScanning()
        }
    }

    private val connectionTimeoutRunnable = Runnable {
        val targetAddr = connectingDeviceAddress
        if (targetAddr != null) {
            _statusLog.value = "Connection attempt to $targetAddr timed out after 15s"
            markPendingMessageFailed("Connection timed out")
            disconnectPeer(targetAddr)
        }
    }

    // State Flows
    private val _hardwareState = MutableStateFlow(BleHardwareState.CHECKING)
    val hardwareState: StateFlow<BleHardwareState> = _hardwareState.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeerDevice>> = _discoveredPeers.asStateFlow()

    private val _connectedPeerAddress = MutableStateFlow<String?>(null)
    val connectedPeerAddress: StateFlow<String?> = _connectedPeerAddress.asStateFlow()

    private val _peerConnectionStates = MutableStateFlow<Map<String, PeerConnectionInfo>>(emptyMap())
    val peerConnectionStates: StateFlow<Map<String, PeerConnectionInfo>> = _peerConnectionStates.asStateFlow()

    private val _nativeMessages = MutableStateFlow<List<NativeBleMessage>>(emptyList())
    val nativeMessages: StateFlow<List<NativeBleMessage>> = _nativeMessages.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _statusLog = MutableStateFlow<String?>("BLE subsystem initialized")
    val statusLog: StateFlow<String?> = _statusLog.asStateFlow()

    private var pendingOutgoingPayload: String? = null
    private var pendingOutgoingMessageId: String? = null
    var persistenceListener: BleMessagePersistenceListener? = null

    fun updatePeerConnectionState(address: String, name: String?, state: PeerConnectionState) {
        val current = _peerConnectionStates.value.toMutableMap()
        current[address.uppercase()] = PeerConnectionInfo(
            state = state,
            peerAddress = address,
            peerName = name ?: current[address.uppercase()]?.peerName,
            lastStateChangeMs = System.currentTimeMillis()
        )
        _peerConnectionStates.value = current
    }

    fun getPeerConnectionState(address: String?): PeerConnectionState {
        if (address == null) return PeerConnectionState.DISCONNECTED
        return _peerConnectionStates.value[address.uppercase()]?.state ?: PeerConnectionState.DISCONNECTED
    }

    init {
        try {
            refreshHardwareState()
        } catch (e: Exception) {
            _hardwareState.value = BleHardwareState.HARDWARE_UNAVAILABLE
            _statusLog.value = "Hardware initialization check caught: ${e.message}"
        }
    }

    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val advGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
            val connGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            scanGranted && advGranted && connGranted
        } else {
            val fineGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fineGranted || coarseGranted
        }
    }

    fun onPermissionDenied() {
        _hardwareState.value = BleHardwareState.PERMISSIONS_REQUIRED
        _lastError.value = "Bluetooth permissions denied. Cannot scan or advertise."
        _statusLog.value = "Permissions denied. BLE discovery and advertising unavailable."
    }

    fun refreshHardwareState() {
        if (!isBleSupported() || bluetoothAdapter == null) {
            _hardwareState.value = BleHardwareState.HARDWARE_UNAVAILABLE
            _statusLog.value = "Bluetooth Low Energy hardware not available on this device"
            return
        }

        if (!hasRequiredPermissions()) {
            _hardwareState.value = BleHardwareState.PERMISSIONS_REQUIRED
            _statusLog.value = "Missing required Android Bluetooth permissions"
            return
        }

        if (!isBluetoothEnabled()) {
            _hardwareState.value = BleHardwareState.BLUETOOTH_DISABLED
            _isScanning.value = false
            _isAdvertising.value = false
            _statusLog.value = "Bluetooth radio is powered OFF"
            return
        }

        if (_isAdvertising.value) {
            _hardwareState.value = BleHardwareState.ADVERTISING
        } else if (_isScanning.value) {
            _hardwareState.value = BleHardwareState.SCANNING
        } else if (_connectedPeerAddress.value != null) {
            _hardwareState.value = BleHardwareState.CONNECTED
        } else {
            _hardwareState.value = BleHardwareState.READY_IDLE
            _statusLog.value = "Radio Ready: Physical BLE adapter operational"
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (!isBleSupported() || bluetoothAdapter == null) {
            _lastError.value = "Cannot advertise: BLE hardware unavailable on this device"
            refreshHardwareState()
            return
        }

        if (!hasRequiredPermissions()) {
            _lastError.value = "Cannot advertise: Bluetooth permissions missing"
            refreshHardwareState()
            return
        }

        if (!isBluetoothEnabled()) {
            _lastError.value = "Cannot advertise: Bluetooth is disabled. Please turn on Bluetooth."
            refreshHardwareState()
            return
        }

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            _lastError.value = "BluetoothLeAdvertiser not supported by this chipset"
            return
        }

        // Initialize local GATT server to receive incoming writes from peers
        setupGattServer()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        // Keep primary advertisement packet compact (under 31 bytes) to fit 128-bit UUID reliably
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        // Place device name in Scan Response to prevent ADVERTISE_FAILED_DATA_TOO_LARGE
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
            _statusLog.value = "Started BLE advertising with Service UUID 0xFD42"
        } catch (e: SecurityException) {
            _lastError.value = "SecurityException starting advertising: ${e.message}"
            _isAdvertising.value = false
        } catch (e: Exception) {
            _lastError.value = "Failed to start advertising: ${e.message}"
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (!hasRequiredPermissions()) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            _statusLog.value = "Stopped BLE advertising"
            refreshHardwareState()
        } catch (e: Exception) {
            // Ignore on cleanup
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            _hardwareState.value = BleHardwareState.ADVERTISING
            _lastError.value = null
            _statusLog.value = "BLE Advertisement active: Broadcast Beacon Online"
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise data exceeds 31-byte limit"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising unsupported on this hardware"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth controller error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many active advertisers"
                else -> "Advertise error code $errorCode"
            }
            _lastError.value = errorMsg
            _statusLog.value = "Advertise failed: $errorMsg"
            refreshHardwareState()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        if (gattServer != null) return
        if (!hasRequiredPermissions()) return

        try {
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                _lastError.value = "Failed to open BluetoothGattServer"
                return
            }

            val service = BluetoothGattService(
                BleConstants.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val messageChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            service.addCharacteristic(messageChar)
            gattServer?.addService(service)
            _statusLog.value = "GATT Server configured with characteristic 0xFD43"
        } catch (e: SecurityException) {
            _lastError.value = "Permission error opening GATT server: ${e.message}"
        } catch (e: Exception) {
            _lastError.value = "Error configuring GATT server: ${e.message}"
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            if (characteristic?.uuid == BleConstants.CHAR_MESSAGE_UUID && value != null) {
                val receivedText = String(value, Charsets.UTF_8)
                val senderAddr = device?.address ?: "UNKNOWN_ADDR"
                val senderName = try {
                    device?.name ?: "Nearby BLE Peer"
                } catch (e: SecurityException) {
                    "Nearby BLE Peer"
                }

                val formatter = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                val timeStr = formatter.format(Date())

                val newMsg = NativeBleMessage(
                    id = "RX_" + UUID.randomUUID().toString().take(6),
                    senderAddress = senderAddr,
                    senderName = senderName,
                    text = receivedText,
                    timestamp = timeStr,
                    isOutgoing = false,
                    deliveryStatus = "Received (Direct BLE)",
                    isAckReceived = true
                )

                _nativeMessages.value = _nativeMessages.value + newMsg
                _statusLog.value = "Received ${value.size} bytes from $senderAddr"
                persistenceListener?.onIncomingMessageReceived(newMsg.id, senderAddr, senderName, receivedText)
            }

            if (responseNeeded && device != null) {
                try {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value
                    )
                } catch (e: SecurityException) {
                    // Ignored
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            val addr = device?.address ?: "Peer"
            val devName = try { device?.name } catch (e: Exception) { null }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedPeerAddress.value = addr
                updatePeerConnectionState(addr, devName, PeerConnectionState.CONNECTED)
                _hardwareState.value = BleHardwareState.CONNECTED
                _statusLog.value = "Incoming GATT connection from $addr"
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_connectedPeerAddress.value == addr) {
                    _connectedPeerAddress.value = null
                }
                updatePeerConnectionState(addr, devName, PeerConnectionState.DISCONNECTED)
                _statusLog.value = "Peer disconnected: $addr"
                refreshHardwareState()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!isBleSupported() || bluetoothAdapter == null) {
            _lastError.value = "Cannot scan: BLE hardware unavailable on this device"
            refreshHardwareState()
            return
        }

        if (!hasRequiredPermissions()) {
            _lastError.value = "Cannot scan: Bluetooth permissions missing"
            refreshHardwareState()
            return
        }

        if (!isBluetoothEnabled()) {
            _lastError.value = "Cannot scan: Bluetooth is disabled. Please turn on Bluetooth."
            refreshHardwareState()
            return
        }

        scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _lastError.value = "BluetoothLeScanner not supported by this device"
            return
        }

        _discoveredPeers.value = emptyList()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), scanSettings, scanCallback)
            _isScanning.value = true
            _hardwareState.value = BleHardwareState.SCANNING
            _statusLog.value = "Started BLE scan for Service UUID 0xFD42 (20s timeout)"

            // Schedule auto scan timeout
            mainHandler.removeCallbacks(scanTimeoutRunnable)
            mainHandler.postDelayed(scanTimeoutRunnable, BleConstants.SCAN_TIMEOUT_MS)
        } catch (e: SecurityException) {
            _lastError.value = "SecurityException starting BLE scan: ${e.message}"
            _isScanning.value = false
        } catch (e: Exception) {
            _lastError.value = "Failed to start BLE scan: ${e.message}"
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        if (!hasRequiredPermissions()) {
            _isScanning.value = false
            return
        }
        try {
            scanner?.stopScan(scanCallback)
            _isScanning.value = false
            _statusLog.value = "Stopped BLE scan"
            refreshHardwareState()
        } catch (e: Exception) {
            _isScanning.value = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val dev = result.device ?: return
            val address = dev.address ?: return

            // Safely parse name from scan record or device name
            val recordName = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            val devName = try {
                dev.name?.takeIf { it.isNotBlank() }
            } catch (e: SecurityException) {
                null
            } catch (e: Exception) {
                null
            }

            val fallbackName = "MeshLink Peer (${address.takeLast(5)})"

            val existingList = _discoveredPeers.value.toMutableList()
            val index = existingList.indexOfFirst { it.address == address }

            // Deduplication & name preservation
            val resolvedName = recordName
                ?: devName
                ?: (if (index >= 0 && !existingList[index].name.startsWith("MeshLink Peer")) {
                    existingList[index].name
                } else {
                    fallbackName
                })

            val updated = DiscoveredPeerDevice(
                device = dev,
                name = resolvedName,
                address = address,
                rssi = result.rssi,
                lastSeenMs = System.currentTimeMillis()
            )

            if (index >= 0) {
                existingList[index] = updated
            } else {
                existingList.add(updated)
            }
            _discoveredPeers.value = existingList
            _statusLog.value = "Discovered peer: $resolvedName (${result.rssi} dBm)"
        }

        override fun onScanFailed(errorCode: Int) {
            mainHandler.removeCallbacks(scanTimeoutRunnable)
            _isScanning.value = false
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already in progress"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal BLE controller error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning unsupported on this hardware"
                else -> "Scan error code $errorCode"
            }
            _lastError.value = errorMsg
            _statusLog.value = "Scan failed: $errorMsg"
            refreshHardwareState()
        }
    }

    @SuppressLint("MissingPermission")
    fun sendDirectBleMessage(targetDevice: BluetoothDevice, messageText: String, customMessageId: String? = null) {
        if (!hasRequiredPermissions()) {
            _lastError.value = "Permissions missing for Bluetooth transmission"
            return
        }

        val trimmedText = messageText.trim()
        if (trimmedText.isEmpty()) return

        val targetAddress = targetDevice.address
        val targetName = try { targetDevice.name } catch (e: Exception) { null }

        // Check if already connecting to this target to avoid duplicate concurrent connects
        if (connectingDeviceAddress.equals(targetAddress, ignoreCase = true) &&
            _peerConnectionStates.value[targetAddress.uppercase()]?.state == PeerConnectionState.CONNECTING) {
            _statusLog.value = "Already attempting connection to $targetAddress"
            return
        }

        val formatter = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timeStr = formatter.format(Date())
        val messageId = customMessageId ?: ("TX_" + UUID.randomUUID().toString().take(6))

        val outgoingMsg = NativeBleMessage(
            id = messageId,
            senderAddress = "This Phone",
            senderName = "Me",
            text = trimmedText,
            timestamp = timeStr,
            isOutgoing = true,
            deliveryStatus = "Connecting...",
            isAckReceived = false
        )

        _nativeMessages.value = _nativeMessages.value + outgoingMsg
        pendingOutgoingPayload = trimmedText
        pendingOutgoingMessageId = messageId

        persistenceListener?.onOutgoingMessageSaved(messageId, targetAddress, trimmedText)

        _hardwareState.value = BleHardwareState.CONNECTING
        connectingDeviceAddress = targetAddress
        updatePeerConnectionState(targetAddress, targetName, PeerConnectionState.CONNECTING)
        _statusLog.value = "Connecting to peer GATT server at $targetAddress..."

        // Start 15s connection timeout safeguard
        mainHandler.removeCallbacks(connectionTimeoutRunnable)
        mainHandler.postDelayed(connectionTimeoutRunnable, 15000L)

        try {
            // Safely close and cleanup any existing client GATT
            activeClientGatt?.close()
            activeClientGatt = null

            activeClientGatt = targetDevice.connectGatt(
                context,
                false,
                clientGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            connectingDeviceAddress = null
            updatePeerConnectionState(targetAddress, targetName, PeerConnectionState.DISCONNECTED)
            _lastError.value = "Permission error connecting GATT: ${e.message}"
            markPendingMessageFailed("Permission error")
        } catch (e: Exception) {
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            connectingDeviceAddress = null
            updatePeerConnectionState(targetAddress, targetName, PeerConnectionState.DISCONNECTED)
            _lastError.value = "Connection error: ${e.message}"
            markPendingMessageFailed("Connection failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectPeer(address: String? = null) {
        mainHandler.removeCallbacks(connectionTimeoutRunnable)
        val peerAddr = address ?: connectingDeviceAddress ?: _connectedPeerAddress.value
        try {
            activeClientGatt?.disconnect()
            activeClientGatt?.close()
            activeClientGatt = null
        } catch (e: Exception) {
            // Ignore
        }
        if (peerAddr != null) {
            updatePeerConnectionState(peerAddr, null, PeerConnectionState.DISCONNECTED)
            if (_connectedPeerAddress.value.equals(peerAddr, ignoreCase = true)) {
                _connectedPeerAddress.value = null
            }
        } else {
            _connectedPeerAddress.value = null
        }
        connectingDeviceAddress = null
        _statusLog.value = "Disconnected from peer ${peerAddr ?: ""}"
        refreshHardwareState()
    }

    private val clientGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            val devAddr = gatt?.device?.address ?: connectingDeviceAddress ?: "Peer"
            val devName = try { gatt?.device?.name } catch (e: Exception) { null }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _statusLog.value = "GATT connection status error: $status on $devAddr"
                updatePeerConnectionState(devAddr, devName, PeerConnectionState.DISCONNECTED)
                markPendingMessageFailed("GATT connection failed (code $status)")
                try {
                    gatt?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                if (activeClientGatt == gatt) {
                    activeClientGatt = null
                }
                connectingDeviceAddress = null
                refreshHardwareState()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedPeerAddress.value = devAddr
                connectingDeviceAddress = null
                updatePeerConnectionState(devAddr, devName, PeerConnectionState.CONNECTED)
                _hardwareState.value = BleHardwareState.CONNECTED
                _statusLog.value = "GATT connected to $devAddr. Discovering services..."
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    markPendingMessageFailed("SecurityException on discoverServices")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_connectedPeerAddress.value == devAddr) {
                    _connectedPeerAddress.value = null
                }
                connectingDeviceAddress = null
                updatePeerConnectionState(devAddr, devName, PeerConnectionState.DISCONNECTED)
                _statusLog.value = "GATT disconnected from peer $devAddr"
                try {
                    gatt?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                if (activeClientGatt == gatt) {
                    activeClientGatt = null
                }
                refreshHardwareState()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                markPendingMessageFailed("Service discovery failed (status $status)")
                return
            }

            val service = gatt.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                markPendingMessageFailed("Target device did not advertise MeshLink Service 0xFD42")
                return
            }

            val characteristic = service.getCharacteristic(BleConstants.CHAR_MESSAGE_UUID)
            if (characteristic == null) {
                markPendingMessageFailed("MeshLink characteristic 0xFD43 not found on peer")
                return
            }

            val payload = pendingOutgoingPayload
            if (payload == null) {
                return
            }

            _hardwareState.value = BleHardwareState.TRANSMITTING
            _statusLog.value = "Writing ${payload.toByteArray().size} bytes to characteristic..."
            pendingOutgoingMessageId?.let { persistenceListener?.onOutgoingMessageTransmitting(it) }

            val payloadBytes = payload.toByteArray(Charsets.UTF_8)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        payloadBytes,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = payloadBytes
                    @Suppress("DEPRECATION")
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
            } catch (e: SecurityException) {
                markPendingMessageFailed("SecurityException writing characteristic")
            } catch (e: Exception) {
                markPendingMessageFailed("Exception writing characteristic: ${e.message}")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val msgId = pendingOutgoingMessageId
                if (msgId != null) {
                    _nativeMessages.value = _nativeMessages.value.map { msg ->
                        if (msg.id == msgId) {
                            msg.copy(
                                deliveryStatus = "Delivered (Direct BLE ACK)",
                                isAckReceived = true
                            )
                        } else msg
                    }
                    persistenceListener?.onOutgoingMessageDelivered(msgId)
                }
                _statusLog.value = "Direct BLE write successful. ACK received from peer."
                pendingOutgoingPayload = null
                pendingOutgoingMessageId = null
                _hardwareState.value = if (_connectedPeerAddress.value != null) BleHardwareState.CONNECTED else BleHardwareState.READY_IDLE
            } else {
                markPendingMessageFailed("Write failed with GATT status $status")
            }
        }
    }

    private fun markPendingMessageFailed(reason: String) {
        mainHandler.removeCallbacks(connectionTimeoutRunnable)
        val msgId = pendingOutgoingMessageId
        if (msgId != null) {
            _nativeMessages.value = _nativeMessages.value.map { msg ->
                if (msg.id == msgId) {
                    msg.copy(deliveryStatus = "Delivery Failed: $reason")
                } else msg
            }
            persistenceListener?.onOutgoingMessageFailed(msgId, reason)
        }
        _lastError.value = reason
        pendingOutgoingPayload = null
        pendingOutgoingMessageId = null
        _hardwareState.value = if (_connectedPeerAddress.value != null) BleHardwareState.CONNECTED else BleHardwareState.READY_IDLE
    }

    fun clearError() {
        _lastError.value = null
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.removeCallbacks(connectionTimeoutRunnable)
        stopAdvertising()
        stopScanning()
        try {
            gattServer?.close()
            gattServer = null
        } catch (e: Exception) {
            // Ignore
        }
        try {
            activeClientGatt?.disconnect()
            activeClientGatt?.close()
            activeClientGatt = null
        } catch (e: Exception) {
            // Ignore
        }
        connectingDeviceAddress = null
    }
}
