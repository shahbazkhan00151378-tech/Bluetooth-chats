package com.example.model

enum class PeerStatus(val label: String) {
    DIRECT_NEIGHBOR("Direct 1-Hop Neighbor"),
    MULTI_HOP_REACHABLE("Multi-Hop Mesh Reachable"),
    ROUTE_SEARCHING("Discovering Route"),
    OUT_OF_RANGE("Out of Radio Range"),
    STANDBY("Radio Standby")
}

data class PeerNode(
    val id: String,
    val displayName: String,
    val publicKeyFingerprint: String,
    val distanceHops: Int,
    val rssi: Int?,
    val status: PeerStatus,
    val relayVia: String? = null,
    val lastSeenMinutesAgo: Int,
    val isRelayCapable: Boolean = true
)

enum class MessageDeliveryStatus(val label: String, val isFinal: Boolean) {
    PENDING("Pending", false),
    SENT("Sent", false),
    DELIVERED("Delivered", true),
    FAILED("Failed", true),
    STORED_LOCALLY("Stored Locally (Room/SQLite) - Awaiting RF Route", false),
    DISCOVERING_ROUTE("Route Discovery in Progress (RREQ)", false),
    RELAYED_HOP("Forwarded (Hop 1/2)", false),
    DELIVERED_PEER("Delivered & E2EE Verified", true),
    BROADCAST_RECEIVED("Mesh Flood Broadcast Received", true)
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderNodeId: String,
    val senderName: String,
    val isOutgoing: Boolean,
    val text: String,
    val timestampFormatted: String,
    val deliveryStatus: MessageDeliveryStatus,
    val hopsTraversed: Int,
    val maxHops: Int = 3,
    val routePath: List<String> = emptyList(),
    val isE2EEncrypted: Boolean = true,
    val estimatedPacketBytes: Int = 128,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSampleDemo: Boolean = false,
    val isAckReceived: Boolean = false
)

data class PersonalConversation(
    val id: String,
    val peer: PeerNode,
    val lastMessage: ChatMessage,
    val unreadCount: Int = 0,
    val isSampleDemo: Boolean = false
)

data class GroupChannel(
    val id: String,
    val name: String,
    val topic: String,
    val hopRadius: Int,
    val estimatedNodesInRange: Int,
    val lastMessage: ChatMessage,
    val channelKeyFingerprint: String
)

enum class RouteStatus(val label: String) {
    ACTIVE("Active Primary Route"),
    SEARCHING_ALTERNATIVE("Self-Healing Route Discovery"),
    STALE("Stale - Route Expired")
}

data class MeshRouteEntry(
    val destinationNodeId: String,
    val destinationLabel: String,
    val nextHopNodeId: String,
    val nextHopLabel: String,
    val hopMetric: Int,
    val linkQuality: String,
    val routeStatus: RouteStatus,
    val lastRenewedSecs: Int
)

data class MeshPacketFrame(
    val packetUuid: String,
    val frameVersion: Int = 1,
    val packetType: String,
    val sourceNode: String,
    val destinationNode: String,
    val timeToLiveTtl: Int,
    val hopCount: Int,
    val payloadSha256: String,
    val rawPayloadHex: String,
    val signaturePreview: String
)

data class TechnicalSpecItem(
    val title: String,
    val category: String,
    val phase1Status: String,
    val nativeAndroidStack: String,
    val explanation: String,
    val limitationsAndChallenges: String
)
