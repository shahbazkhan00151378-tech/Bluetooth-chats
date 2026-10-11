package com.example.ble

import java.util.UUID

object BleConstants {
    // Dedicated MeshLink 128-bit Service UUID (using 16-bit alias 0xFD42 base)
    val SERVICE_UUID: UUID = UUID.fromString("0000FD42-0000-1000-8000-00805F9B34FB")

    // Characteristic for transmitting/receiving text message payloads (Read/Write)
    val CHAR_MESSAGE_UUID: UUID = UUID.fromString("0000FD43-0000-1000-8000-00805F9B34FB")

    // Characteristic for delivery acknowledgement and status verification
    val CHAR_ACK_UUID: UUID = UUID.fromString("0000FD44-0000-1000-8000-00805F9B34FB")

    // Standard BLE unfragmented payload size limit (Default ATT MTU 23 - 3 bytes header = 20 bytes)
    const val DEFAULT_MAX_PAYLOAD_BYTES = 20

    // Expanded MTU target for negotiated BLE connections
    const val TARGET_NEGOTIATED_MTU = 512

    // Auto scan timeout in milliseconds (20 seconds)
    const val SCAN_TIMEOUT_MS = 20_000L
}
