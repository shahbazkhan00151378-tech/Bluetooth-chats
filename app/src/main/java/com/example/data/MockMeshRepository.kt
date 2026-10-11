package com.example.data

import android.content.Context
import com.example.data.local.MeshChatDatabase
import com.example.model.ChatMessage
import com.example.model.GroupChannel
import com.example.model.MessageDeliveryStatus
import com.example.model.MeshPacketFrame
import com.example.model.MeshRouteEntry
import com.example.model.PeerNode
import com.example.model.PeerStatus
import com.example.model.PersonalConversation
import com.example.model.RouteStatus
import com.example.model.TechnicalSpecItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MockMeshRepository(
    val context: Context? = null
) {
    val localNodeId = "NODE_F8A2_LOCAL"
    val localNodeName = "My Device (This Phone)"
    val localKeyFingerprint = "ed25519:e48a..c901"

    val chatRepository: MeshChatRepository? = context?.let {
        MeshChatRepository(MeshChatDatabase.getInstance(it))
    }

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _conversations = MutableStateFlow<List<PersonalConversation>>(emptyList())
    val conversations: StateFlow<List<PersonalConversation>> = _conversations.asStateFlow()

    private val _groupChannels = MutableStateFlow<List<GroupChannel>>(emptyList())
    val groupChannels: StateFlow<List<GroupChannel>> = _groupChannels.asStateFlow()

    private val _routeTable = MutableStateFlow<List<MeshRouteEntry>>(emptyList())
    val routeTable: StateFlow<List<MeshRouteEntry>> = _routeTable.asStateFlow()

    private val _discoveredNodes = MutableStateFlow<List<PeerNode>>(emptyList())
    val discoveredNodes: StateFlow<List<PeerNode>> = _discoveredNodes.asStateFlow()

    // In-memory conversation messages map
    private val _messagesByConversation = mutableMapOf<String, MutableList<ChatMessage>>()

    init {
        loadMockData()
        if (chatRepository != null) {
            repoScope.launch {
                try {
                    chatRepository.initializeSampleDataIfEmpty()
                    chatRepository.conversations.collect { dbConversations ->
                        if (dbConversations.isNotEmpty()) {
                            _conversations.value = dbConversations
                        }
                    }
                } catch (e: Exception) {
                    // Safe fallback to in-memory mock data
                }
            }
        }
    }

    private fun loadMockData() {
        // Discovered mock peers
        val peerSarah = PeerNode(
            id = "NODE_9A4C",
            displayName = "Sarah Chen",
            publicKeyFingerprint = "ed25519:7b3a..410f",
            distanceHops = 1,
            rssi = -64,
            status = PeerStatus.DIRECT_NEIGHBOR,
            relayVia = null,
            lastSeenMinutesAgo = 2,
            isRelayCapable = true
        )

        val peerMarcus = PeerNode(
            id = "NODE_88B1",
            displayName = "Marcus Vance",
            publicKeyFingerprint = "ed25519:3c91..99d2",
            distanceHops = 2,
            rssi = null,
            status = PeerStatus.MULTI_HOP_REACHABLE,
            relayVia = "NODE_9A4C (Sarah)",
            lastSeenMinutesAgo = 6,
            isRelayCapable = true
        )

        val peerElena = PeerNode(
            id = "NODE_31FE",
            displayName = "Elena Rostova",
            publicKeyFingerprint = "ed25519:12ff..e41a",
            distanceHops = 3,
            rssi = null,
            status = PeerStatus.ROUTE_SEARCHING,
            relayVia = "NODE_4B07 (Relay Outpost)",
            lastSeenMinutesAgo = 14,
            isRelayCapable = false
        )

        val peerLiam = PeerNode(
            id = "NODE_04D7",
            displayName = "Dr. Liam Scott",
            publicKeyFingerprint = "ed25519:aa02..88b4",
            distanceHops = 0,
            rssi = null,
            status = PeerStatus.OUT_OF_RANGE,
            relayVia = null,
            lastSeenMinutesAgo = 120,
            isRelayCapable = false
        )

        _discoveredNodes.value = listOf(peerSarah, peerMarcus, peerElena, peerLiam)

        // Mock messages for Sarah
        val sarahMessages = mutableListOf(
            ChatMessage(
                id = "MSG_001",
                conversationId = peerSarah.id,
                senderNodeId = peerSarah.id,
                senderName = peerSarah.displayName,
                isOutgoing = false,
                text = "Cell towers appear to be completely down across the valley. Switched to MeshLink.",
                timestampFormatted = "10:14 AM",
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER,
                hopsTraversed = 1,
                routePath = listOf(peerSarah.id, localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = 94
            ),
            ChatMessage(
                id = "MSG_002",
                conversationId = peerSarah.id,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Copy that. My phone is picking up your BLE advertisement on direct RF link.",
                timestampFormatted = "10:16 AM",
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER,
                hopsTraversed = 1,
                routePath = listOf(localNodeId, peerSarah.id),
                isE2EEncrypted = true,
                estimatedPacketBytes = 112
            ),
            ChatMessage(
                id = "MSG_003",
                conversationId = peerSarah.id,
                senderNodeId = peerSarah.id,
                senderName = peerSarah.displayName,
                isOutgoing = false,
                text = "I have Marcus linked 1 hop behind me. We can relay messages through my device if he stays within 30 meters.",
                timestampFormatted = "10:19 AM",
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER,
                hopsTraversed = 1,
                routePath = listOf(peerSarah.id, localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = 138
            )
        )
        _messagesByConversation[peerSarah.id] = sarahMessages

        // Mock messages for Marcus (Multi-hop)
        val marcusMessages = mutableListOf(
            ChatMessage(
                id = "MSG_101",
                conversationId = peerMarcus.id,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Marcus, checking multi-hop packet forwarding through Sarah's node.",
                timestampFormatted = "10:22 AM",
                deliveryStatus = MessageDeliveryStatus.RELAYED_HOP,
                hopsTraversed = 2,
                routePath = listOf(localNodeId, peerSarah.id, peerMarcus.id),
                isE2EEncrypted = true,
                estimatedPacketBytes = 104
            ),
            ChatMessage(
                id = "MSG_102",
                conversationId = peerMarcus.id,
                senderNodeId = peerMarcus.id,
                senderName = peerMarcus.displayName,
                isOutgoing = false,
                text = "Received through Sarah's relay. 2 hops total, end-to-end encrypted.",
                timestampFormatted = "10:24 AM",
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER,
                hopsTraversed = 2,
                routePath = listOf(peerMarcus.id, peerSarah.id, localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = 98
            )
        )
        _messagesByConversation[peerMarcus.id] = marcusMessages

        // Mock messages for Elena (Pending Route)
        val elenaMessages = mutableListOf(
            ChatMessage(
                id = "MSG_201",
                conversationId = peerElena.id,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Do you have the medical supplies checklist? (Stored locally, awaiting mesh path)",
                timestampFormatted = "09:45 AM",
                deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY,
                hopsTraversed = 0,
                routePath = listOf(localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = 118
            )
        )
        _messagesByConversation[peerElena.id] = elenaMessages

        // Mock messages for Liam (Out of Range)
        val liamMessages = mutableListOf(
            ChatMessage(
                id = "MSG_301",
                conversationId = peerLiam.id,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Dr. Liam, please advise when your outpost node comes online.",
                timestampFormatted = "Yesterday",
                deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY,
                hopsTraversed = 0,
                routePath = listOf(localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = 88
            )
        )
        _messagesByConversation[peerLiam.id] = liamMessages

        _conversations.value = listOf(
            PersonalConversation(
                id = peerSarah.id,
                peer = peerSarah,
                lastMessage = sarahMessages.last(),
                unreadCount = 0,
                isSampleDemo = true
            ),
            PersonalConversation(
                id = peerMarcus.id,
                peer = peerMarcus,
                lastMessage = marcusMessages.last(),
                unreadCount = 1,
                isSampleDemo = true
            ),
            PersonalConversation(
                id = peerElena.id,
                peer = peerElena,
                lastMessage = elenaMessages.last(),
                unreadCount = 0,
                isSampleDemo = true
            ),
            PersonalConversation(
                id = peerLiam.id,
                peer = peerLiam,
                lastMessage = liamMessages.last(),
                unreadCount = 0,
                isSampleDemo = true
            )
        )

        // Mock Nearby Group Channels
        val emergencyGroupMsg = ChatMessage(
            id = "GRP_MSG_01",
            conversationId = "GRP_EMERGENCY",
            senderNodeId = "NODE_9A4C",
            senderName = "Sarah Chen",
            isOutgoing = false,
            text = "[FLOOD-NET] River road bridge is flooded. Please route west.",
            timestampFormatted = "10:30 AM",
            deliveryStatus = MessageDeliveryStatus.BROADCAST_RECEIVED,
            hopsTraversed = 2,
            maxHops = 5,
            routePath = listOf("NODE_9A4C", "NODE_LOCAL_7F31"),
            isE2EEncrypted = false,
            estimatedPacketBytes = 82
        )

        val campGroupMsg = ChatMessage(
            id = "GRP_MSG_02",
            conversationId = "GRP_CAMP_ALPHA",
            senderNodeId = "NODE_88B1",
            senderName = "Marcus Vance",
            isOutgoing = false,
            text = "Generator fuel reserve at 65%. Radio test successful.",
            timestampFormatted = "10:11 AM",
            deliveryStatus = MessageDeliveryStatus.BROADCAST_RECEIVED,
            hopsTraversed = 1,
            maxHops = 3,
            routePath = listOf("NODE_88B1", "NODE_LOCAL_7F31"),
            isE2EEncrypted = false,
            estimatedPacketBytes = 76
        )

        val weatherGroupMsg = ChatMessage(
            id = "GRP_MSG_03",
            conversationId = "GRP_LOCAL_MESH",
            senderNodeId = localNodeId,
            senderName = "Me",
            isOutgoing = true,
            text = "Broadcast test beacon: Local node operational.",
            timestampFormatted = "09:15 AM",
            deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY,
            hopsTraversed = 0,
            maxHops = 3,
            routePath = listOf(localNodeId),
            isE2EEncrypted = false,
            estimatedPacketBytes = 68
        )

        _groupChannels.value = listOf(
            GroupChannel(
                id = "GRP_EMERGENCY",
                name = "# Emergency Net (All Hops)",
                topic = "Mesh-wide flood broadcast for priority emergency alerts",
                hopRadius = 5,
                estimatedNodesInRange = 8,
                lastMessage = emergencyGroupMsg,
                channelKeyFingerprint = "public-mesh-beacon"
            ),
            GroupChannel(
                id = "GRP_CAMP_ALPHA",
                name = "# Camp Alpha Perimeter",
                topic = "Tactical coordination for base perimeter nodes",
                hopRadius = 3,
                estimatedNodesInRange = 5,
                lastMessage = campGroupMsg,
                channelKeyFingerprint = "grp-aes256:88fa..310e"
            ),
            GroupChannel(
                id = "GRP_LOCAL_MESH",
                name = "# Local Neighborhood Mesh",
                topic = "Community off-grid radio chat (hop radius: 2)",
                hopRadius = 2,
                estimatedNodesInRange = 4,
                lastMessage = weatherGroupMsg,
                channelKeyFingerprint = "public-mesh-beacon"
            )
        )

        _messagesByConversation["GRP_EMERGENCY"] = mutableListOf(emergencyGroupMsg)
        _messagesByConversation["GRP_CAMP_ALPHA"] = mutableListOf(campGroupMsg)
        _messagesByConversation["GRP_LOCAL_MESH"] = mutableListOf(weatherGroupMsg)

        // Mock Routing Table
        _routeTable.value = listOf(
            MeshRouteEntry(
                destinationNodeId = "NODE_9A4C",
                destinationLabel = "Sarah Chen",
                nextHopNodeId = "NODE_9A4C",
                nextHopLabel = "Sarah Chen (Direct BLE)",
                hopMetric = 1,
                linkQuality = "-64 dBm (High)",
                routeStatus = RouteStatus.ACTIVE,
                lastRenewedSecs = 25
            ),
            MeshRouteEntry(
                destinationNodeId = "NODE_88B1",
                destinationLabel = "Marcus Vance",
                nextHopNodeId = "NODE_9A4C",
                nextHopLabel = "Sarah Chen (Relay)",
                hopMetric = 2,
                linkQuality = "Forwarded (-72 dBm)",
                routeStatus = RouteStatus.ACTIVE,
                lastRenewedSecs = 48
            ),
            MeshRouteEntry(
                destinationNodeId = "NODE_31FE",
                destinationLabel = "Elena Rostova",
                nextHopNodeId = "NODE_4B07",
                nextHopLabel = "Relay Outpost #4",
                hopMetric = 3,
                linkQuality = "Degraded / Unstable",
                routeStatus = RouteStatus.SEARCHING_ALTERNATIVE,
                lastRenewedSecs = 180
            ),
            MeshRouteEntry(
                destinationNodeId = "NODE_04D7",
                destinationLabel = "Dr. Liam Scott",
                nextHopNodeId = "NONE",
                nextHopLabel = "No Active Route",
                hopMetric = 99,
                linkQuality = "No Signal",
                routeStatus = RouteStatus.STALE,
                lastRenewedSecs = 7200
            )
        )
    }

    fun addLocalMessage(msg: ChatMessage) {
        val list = _messagesByConversation.getOrPut(msg.conversationId) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == msg.id }
        if (idx >= 0) {
            list[idx] = msg
        } else {
            list.add(msg)
        }

        _conversations.value = _conversations.value.map { c ->
            if (c.id == msg.conversationId) c.copy(lastMessage = msg) else c
        }
    }

    fun getMessagesForConversation(convId: String): List<ChatMessage> {
        return _messagesByConversation[convId] ?: emptyList()
    }

    fun getMessagesFlow(convId: String): Flow<List<ChatMessage>> {
        return chatRepository?.getMessagesFlow(convId)
            ?: kotlinx.coroutines.flow.flowOf(_messagesByConversation[convId] ?: emptyList())
    }

    suspend fun loadMessagesForConversation(convId: String): List<ChatMessage> {
        val dbMessages = chatRepository?.getMessagesSnapshot(convId)
        if (dbMessages != null && dbMessages.isNotEmpty()) {
            _messagesByConversation[convId] = dbMessages.toMutableList()
            return dbMessages
        }
        return _messagesByConversation[convId] ?: emptyList()
    }

    /**
     * Requirement 6: Save outgoing messages locally before attempting BLE delivery.
     */
    suspend fun saveOutgoingPersistentMessage(
        convId: String,
        text: String,
        isGroup: Boolean = false,
        customId: String? = null
    ): ChatMessage {
        val msg = if (chatRepository != null) {
            chatRepository.saveOutgoingMessage(convId, text, isGroup, customId)
        } else {
            sendLocalDraftMessage(convId, text, isGroup)
        }

        val list = _messagesByConversation.getOrPut(convId) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == msg.id }
        if (idx >= 0) {
            list[idx] = msg
        } else {
            list.add(msg)
        }

        _conversations.value = _conversations.value.map { c ->
            if (c.id == convId) c.copy(lastMessage = msg) else c
        }

        return msg
    }

    /**
     * Requirement 7: Save incoming messages locally when received through real BLE message handler.
     */
    suspend fun saveIncomingBleMessage(
        senderAddr: String,
        senderName: String,
        text: String,
        customId: String? = null
    ): ChatMessage {
        val msg = if (chatRepository != null) {
            chatRepository.saveIncomingBleMessage(senderAddr, senderName, text, customId)
        } else {
            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeStr = formatter.format(Date())
            ChatMessage(
                id = customId ?: ("RX_" + UUID.randomUUID().toString().take(6)),
                conversationId = senderAddr,
                senderNodeId = senderAddr,
                senderName = senderName,
                isOutgoing = false,
                text = text,
                timestampFormatted = timeStr,
                deliveryStatus = MessageDeliveryStatus.DELIVERED,
                hopsTraversed = 1,
                routePath = listOf(senderAddr, localNodeId),
                isE2EEncrypted = true,
                estimatedPacketBytes = text.toByteArray().size + 42,
                isAckReceived = true
            )
        }

        val list = _messagesByConversation.getOrPut(senderAddr) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == msg.id }
        if (idx >= 0) {
            list[idx] = msg
        } else {
            list.add(msg)
        }

        val convExists = _conversations.value.any { it.id == senderAddr }
        if (!convExists) {
            val newConv = PersonalConversation(
                id = senderAddr,
                peer = PeerNode(
                    id = senderAddr,
                    displayName = senderName,
                    publicKeyFingerprint = "ed25519:" + UUID.randomUUID().toString().take(8),
                    distanceHops = 1,
                    rssi = -65,
                    status = PeerStatus.DIRECT_NEIGHBOR,
                    relayVia = null,
                    lastSeenMinutesAgo = 0,
                    isRelayCapable = true
                ),
                lastMessage = msg,
                unreadCount = 1,
                isSampleDemo = false
            )
            _conversations.value = listOf(newConv) + _conversations.value
        } else {
            _conversations.value = _conversations.value.map { c ->
                if (c.id == senderAddr) c.copy(lastMessage = msg) else c
            }
        }

        return msg
    }

    /**
     * Requirement 8: Update delivery state accurately (Pending, Sent, Delivered, Failed).
     */
    suspend fun updateDeliveryStatus(
        messageId: String,
        convId: String?,
        status: MessageDeliveryStatus,
        isAckReceived: Boolean
    ) {
        chatRepository?.updateDeliveryStatus(messageId, status, isAckReceived)
        convId?.let { cid ->
            val list = _messagesByConversation[cid]
            if (list != null) {
                val idx = list.indexOfFirst { it.id == messageId }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(deliveryStatus = status, isAckReceived = isAckReceived)
                }
            }
        }
    }

    suspend fun getOrCreateRealConversation(peerAddress: String, peerName: String): PersonalConversation {
        val existing = _conversations.value.find { it.id == peerAddress }
        if (existing != null) return existing

        val created = chatRepository?.getOrCreateRealConversation(peerAddress, peerName)
            ?: PersonalConversation(
                id = peerAddress,
                peer = PeerNode(
                    id = peerAddress,
                    displayName = peerName,
                    publicKeyFingerprint = "ed25519:" + UUID.randomUUID().toString().take(8),
                    distanceHops = 1,
                    rssi = -60,
                    status = PeerStatus.DIRECT_NEIGHBOR,
                    relayVia = null,
                    lastSeenMinutesAgo = 0,
                    isRelayCapable = true
                ),
                lastMessage = ChatMessage(
                    id = "MSG_INIT_" + peerAddress,
                    conversationId = peerAddress,
                    senderNodeId = peerAddress,
                    senderName = peerName,
                    isOutgoing = false,
                    text = "Tap to start direct BLE offline chat",
                    timestampFormatted = "Just now",
                    deliveryStatus = MessageDeliveryStatus.PENDING,
                    hopsTraversed = 1
                ),
                unreadCount = 0,
                isSampleDemo = false
            )

        _conversations.value = listOf(created) + _conversations.value
        return created
    }

    // Adds a mock message locally in Phase 1 (clearly marked as stored locally in Room/SQLite, awaiting RF transmission)
    fun sendLocalDraftMessage(convId: String, text: String, isGroup: Boolean): ChatMessage {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = formatter.format(Date())

        val newMsg = ChatMessage(
            id = "DRAFT_" + UUID.randomUUID().toString().take(6),
            conversationId = convId,
            senderNodeId = localNodeId,
            senderName = "Me",
            isOutgoing = true,
            text = text,
            timestampFormatted = timeStr,
            deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY,
            hopsTraversed = 0,
            maxHops = if (isGroup) 4 else 3,
            routePath = listOf(localNodeId),
            isE2EEncrypted = !isGroup,
            estimatedPacketBytes = text.toByteArray().size + 42
        )

        val list = _messagesByConversation.getOrPut(convId) { mutableListOf() }
        list.add(newMsg)

        // Update conversation last message
        _conversations.value = _conversations.value.map { c ->
            if (c.id == convId) c.copy(lastMessage = newMsg) else c
        }
        _groupChannels.value = _groupChannels.value.map { g ->
            if (g.id == convId) g.copy(lastMessage = newMsg) else g
        }

        if (chatRepository != null) {
            repoScope.launch {
                try {
                    chatRepository.saveOutgoingMessage(convId, text, isGroup, newMsg.id)
                } catch (e: Exception) {
                    // Safe Room fallback
                }
            }
        }

        return newMsg
    }

    fun generatePacketFrame(msg: ChatMessage): MeshPacketFrame {
        val rawHex = msg.text.toByteArray().joinToString("") { "%02X".format(it) }
        val payloadHash = "sha256:" + UUID.randomUUID().toString().replace("-", "").take(16)
        return MeshPacketFrame(
            packetUuid = "PKT_" + msg.id,
            frameVersion = 1,
            packetType = if (msg.conversationId.startsWith("GRP_")) "MESH_BROADCAST_DATA" else "P2P_UNICAST_DATA",
            sourceNode = msg.senderNodeId,
            destinationNode = msg.conversationId,
            timeToLiveTtl = msg.maxHops - msg.hopsTraversed,
            hopCount = msg.hopsTraversed,
            payloadSha256 = payloadHash,
            rawPayloadHex = rawHex.take(32) + if (rawHex.length > 32) "..." else "",
            signaturePreview = "ed25519_sig:" + UUID.randomUUID().toString().take(12)
        )
    }

    fun getTechnicalSpecifications(): List<TechnicalSpecItem> {
        return listOf(
            TechnicalSpecItem(
                title = "1. Device Discovery & Advertisements",
                category = "Physical / Radio Layer",
                phase1Status = "UI-Only Prototype (Mock Data)",
                nativeAndroidStack = "BluetoothLeScanner, BluetoothLeAdvertiser, ScanCallback, AdvertiseCallback",
                explanation = "In Phase 2, phones broadcast compact BLE Service UUID beacons (128-bit) using AdvertiseSettings.ADVERTISE_MODE_BALANCED. Nearby phones continuously scan with ScanFilter to identify peer Node IDs without pairing.",
                limitationsAndChallenges = "Android OEM power management restricts background BLE scanning; scan windows are truncated when screen is off unless FOREGROUND_SERVICE_CONNECTED_DEVICE is held. Scan results can be delayed by 5-15 seconds."
            ),
            TechnicalSpecItem(
                title = "2. Device-to-Device Transport",
                category = "Transport Layer",
                phase1Status = "UI-Only Prototype",
                nativeAndroidStack = "BLE L2CAP Connection-Oriented Channels (CoC) & GATT Server/Client fallback",
                explanation = "Direct transfers between 1-hop neighbors use raw L2CAP CoC sockets (available since Android 10/API 29) for lower latency and high MTU (up to 512+ bytes) without GATT attribute overhead. GATT is retained for legacy compatibility.",
                limitationsAndChallenges = "Android hardware chips allow only 4 to 7 concurrent BLE connections per phone. A true mesh must multiplex short transient connections (connect, exchange, disconnect) rather than permanent connections."
            ),
            TechnicalSpecItem(
                title = "3. Multi-Hop Forwarding & TTL",
                category = "Network / Mesh Layer",
                phase1Status = "UI-Only Prototype",
                nativeAndroidStack = "Decentralized Packet Forwarder Daemon + Room SQLite Queue",
                explanation = "Every forwarded packet contains a Time-To-Live (TTL, default 4). When an intermediate node receives a packet not addressed to itself, it decrements the TTL by 1, appends its node ID to the hop audit path, and re-broadcasts or routes to the next hop.",
                limitationsAndChallenges = "Broadcast storms can saturate the 2.4 GHz spectrum if too many devices forward simultaneously. Jitter backoff timers (random 50-200ms delay) are essential before retransmitting."
            ),
            TechnicalSpecItem(
                title = "4. Duplicate-Message Suppression",
                category = "Routing & Network Layer",
                phase1Status = "UI-Only Prototype",
                nativeAndroidStack = "In-memory LRU Bloom Filter + 64-bit Packet UUID hash cache",
                explanation = "Prevents broadcast packet loops in mesh flooding. When any packet arrives, its (OriginNodeID + SequenceNumber) hash is checked against an in-memory Bloom filter. If already seen within the last 10 minutes, the packet is instantly dropped.",
                limitationsAndChallenges = "Bloom filters consume minimal RAM (~64KB for 100,000 packets) with zero disk I/O, preventing relay nodes from wasting battery on redundant retransmissions."
            ),
            TechnicalSpecItem(
                title = "5. Self-Healing Route Discovery",
                category = "Routing Layer",
                phase1Status = "UI-Only Prototype",
                nativeAndroidStack = "Ad-hoc On-Demand Distance Vector (AODV) / Dynamic Source Routing (DSR)",
                explanation = "When a relay node drops off or moves out of range, the transmitting node sends a Route Request (RREQ) broadcast. Any node with an active path to the destination replies with Route Reply (RREP). Alternative routes are automatically installed into the local routing table.",
                limitationsAndChallenges = "High mobility causes frequent route breaks. The routing engine must treat routes as soft-state with aggressive decay (60-120 seconds) and fast fallback to scoped broadcast flooding."
            ),
            TechnicalSpecItem(
                title = "6. End-to-End Encryption (E2EE)",
                category = "Cryptographic Security",
                phase1Status = "UI-Only Prototype (Representational Keys)",
                nativeAndroidStack = "Tink / BouncyCastle / libsodium (X25519 ECDH + ChaCha20-Poly1305 + Ed25519 Signatures)",
                explanation = "1-on-1 messages are encrypted end-to-end between origin and destination nodes. Intermediate relay phones only inspect the unencrypted Routing Header (Source, Destination, TTL, Packet ID). They cannot decrypt or tamper with the payload.",
                limitationsAndChallenges = "Requires out-of-band trust bootstrapping: users must scan a QR code or compare 6-word safety numbers in person to prevent man-in-the-middle attacks, because there is no central PKI or certificate authority."
            ),
            TechnicalSpecItem(
                title = "7. Local Storage & Offline Durability",
                category = "Storage Layer",
                phase1Status = "UI-Only Prototype",
                nativeAndroidStack = "Android Room Database (SQLite) + SQLCipher for encrypted database at rest",
                explanation = "Messages and peer cryptographic keys are stored in encrypted SQLite tables. Outgoing messages enter an 'Awaiting Route' queue. When a peer or suitable relay advertises nearby, the queue flushes automatically.",
                limitationsAndChallenges = "Storage space is finite on low-end devices. Automatic compaction of message payloads and expiration of unroutable ephemeral packets after 24 hours is required."
            )
        )
    }
}
