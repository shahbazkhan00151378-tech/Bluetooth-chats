package com.example.ble

import android.bluetooth.BluetoothDevice

enum class BleHardwareState(val label: String) {
    CHECKING("Checking Radio Hardware..."),
    HARDWARE_UNAVAILABLE("No Bluetooth Hardware Detected"),
    BLUETOOTH_DISABLED("Bluetooth Radio Disabled"),
    PERMISSIONS_REQUIRED("Bluetooth Permissions Required"),
    READY_IDLE("Radio Ready (Idle)"),
    ADVERTISING("Advertising Mesh Presence"),
    SCANNING("Scanning for Nearby MeshLink Devices"),
    CONNECTING("Connecting to GATT Server..."),
    CONNECTED("Connected to Peer GATT"),
    TRANSMITTING("Transmitting Message Frame...")
}

enum class PeerConnectionState(val label: String) {
    DISCOVERED("Discovered (Not Connected)"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected")
}

data class PeerConnectionInfo(
    val state: PeerConnectionState = PeerConnectionState.DISCONNECTED,
    val peerAddress: String? = null,
    val peerName: String? = null,
    val lastStateChangeMs: Long = System.currentTimeMillis()
)

data class DiscoveredPeerDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class NativeBleMessage(
    val id: String,
    val senderAddress: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isOutgoing: Boolean,
    val deliveryStatus: String, // "Queued", "Sending...", "Delivered (Direct BLE ACK)", "Failed"
    val isAckReceived: Boolean = false
)

interface BleMessagePersistenceListener {
    fun onOutgoingMessageSaved(messageId: String, peerAddress: String, text: String)
    fun onOutgoingMessageTransmitting(messageId: String)
    fun onOutgoingMessageDelivered(messageId: String)
    fun onOutgoingMessageFailed(messageId: String, reason: String)
    fun onIncomingMessageReceived(messageId: String, senderAddress: String, senderName: String, text: String)
}
