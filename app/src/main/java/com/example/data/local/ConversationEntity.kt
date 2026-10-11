package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["lastMessageTimestampMs"]),
        Index(value = ["isSampleDemo"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val peerId: String,
    val displayName: String,
    val peerAddress: String? = null,
    val publicKeyFingerprint: String = "",
    val distanceHops: Int = 1,
    val rssi: Int? = null,
    val peerStatus: String = "DIRECT_NEIGHBOR",
    val relayVia: String? = null,
    val lastSeenMinutesAgo: Int = 0,
    val isRelayCapable: Boolean = true,
    val lastMessageId: String? = null,
    val lastMessageText: String = "",
    val lastMessageTimestampFormatted: String = "",
    val lastMessageTimestampMs: Long = 0L,
    val lastDeliveryStatus: String = "PENDING",
    val unreadCount: Int = 0,
    val isSampleDemo: Boolean = false,
    val isGroup: Boolean = false
)
