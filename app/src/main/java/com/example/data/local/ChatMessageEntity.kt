package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestampMs"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderNodeId: String,
    val senderName: String,
    val isOutgoing: Boolean,
    val text: String,
    val timestampFormatted: String,
    val timestampMs: Long,
    val deliveryStatus: String, // PENDING, SENT, DELIVERED, FAILED, STORED_LOCALLY, etc.
    val isAckReceived: Boolean = false,
    val isSampleDemo: Boolean = false,
    val hopsTraversed: Int = 0,
    val maxHops: Int = 3,
    val routePath: String = "",
    val isE2EEncrypted: Boolean = true,
    val estimatedPacketBytes: Int = 128
)
