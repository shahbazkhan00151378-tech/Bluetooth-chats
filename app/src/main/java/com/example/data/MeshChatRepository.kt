package com.example.data

import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MeshChatDatabase
import com.example.model.ChatMessage
import com.example.model.MessageDeliveryStatus
import com.example.model.PeerNode
import com.example.model.PeerStatus
import com.example.model.PersonalConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MeshChatRepository(
    private val database: MeshChatDatabase
) {
    private val chatMessageDao: ChatMessageDao = database.chatMessageDao()
    private val conversationDao: ConversationDao = database.conversationDao()

    val localNodeId = "NODE_F8A2_LOCAL"
    val localNodeName = "My Device (This Phone)"

    // Flow of all conversations mapped to domain model
    val conversations: Flow<List<PersonalConversation>> =
        conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }

    val realConversations: Flow<List<PersonalConversation>> =
        conversationDao.getRealConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }

    val sampleConversations: Flow<List<PersonalConversation>> =
        conversationDao.getSampleConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getMessagesSnapshot(conversationId: String): List<ChatMessage> {
        return chatMessageDao.getMessagesForConversationList(conversationId).map { it.toDomainModel() }
    }

    suspend fun getConversationById(conversationId: String): PersonalConversation? {
        return conversationDao.getConversationById(conversationId)?.toDomainModel()
    }

    /**
     * Requirement 6: Save outgoing message locally before attempting BLE delivery.
     * Starts with delivery status PENDING.
     */
    suspend fun saveOutgoingMessage(
        conversationId: String,
        text: String,
        isGroup: Boolean = false,
        customId: String? = null
    ): ChatMessage {
        val timestampMs = System.currentTimeMillis()
        val timeFormatted = formatTime(timestampMs)
        val msgId = customId ?: ("TX_" + UUID.randomUUID().toString().take(6))

        val entity = ChatMessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderNodeId = localNodeId,
            senderName = "Me",
            isOutgoing = true,
            text = text,
            timestampFormatted = timeFormatted,
            timestampMs = timestampMs,
            deliveryStatus = MessageDeliveryStatus.PENDING.name,
            isAckReceived = false,
            isSampleDemo = false,
            hopsTraversed = 0,
            maxHops = if (isGroup) 4 else 3,
            routePath = localNodeId,
            isE2EEncrypted = !isGroup,
            estimatedPacketBytes = text.toByteArray(Charsets.UTF_8).size + 42
        )

        chatMessageDao.insertMessage(entity)

        // Ensure conversation exists or update its last message
        val existingConv = conversationDao.getConversationById(conversationId)
        if (existingConv != null) {
            conversationDao.updateLastMessage(
                convId = conversationId,
                lastMsgId = msgId,
                text = text,
                timeFormatted = timeFormatted,
                timestampMs = timestampMs,
                status = MessageDeliveryStatus.PENDING.name
            )
        } else {
            // Create real conversation for this recipient
            val newConv = ConversationEntity(
                id = conversationId,
                peerId = conversationId,
                displayName = "Peer ($conversationId)",
                peerAddress = conversationId,
                publicKeyFingerprint = "ed25519:" + UUID.randomUUID().toString().take(8),
                distanceHops = 1,
                rssi = -70,
                peerStatus = PeerStatus.DIRECT_NEIGHBOR.name,
                relayVia = null,
                lastSeenMinutesAgo = 0,
                isRelayCapable = true,
                lastMessageId = msgId,
                lastMessageText = text,
                lastMessageTimestampFormatted = timeFormatted,
                lastMessageTimestampMs = timestampMs,
                lastDeliveryStatus = MessageDeliveryStatus.PENDING.name,
                unreadCount = 0,
                isSampleDemo = false,
                isGroup = isGroup
            )
            conversationDao.insertConversation(newConv)
        }

        return entity.toDomainModel()
    }

    /**
     * Requirement 7: Save incoming message locally when received through the real BLE message handler.
     */
    suspend fun saveIncomingBleMessage(
        senderAddress: String,
        senderName: String,
        text: String,
        customId: String? = null
    ): ChatMessage {
        val timestampMs = System.currentTimeMillis()
        val timeFormatted = formatTime(timestampMs)
        val msgId = customId ?: ("RX_" + UUID.randomUUID().toString().take(6))

        val entity = ChatMessageEntity(
            id = msgId,
            conversationId = senderAddress,
            senderNodeId = senderAddress,
            senderName = senderName,
            isOutgoing = false,
            text = text,
            timestampFormatted = timeFormatted,
            timestampMs = timestampMs,
            deliveryStatus = MessageDeliveryStatus.DELIVERED.name,
            isAckReceived = true,
            isSampleDemo = false,
            hopsTraversed = 1,
            maxHops = 3,
            routePath = "$senderAddress,$localNodeId",
            isE2EEncrypted = true,
            estimatedPacketBytes = text.toByteArray(Charsets.UTF_8).size + 42
        )

        chatMessageDao.insertMessage(entity)

        val existingConv = conversationDao.getConversationById(senderAddress)
        if (existingConv != null) {
            conversationDao.updateLastMessage(
                convId = senderAddress,
                lastMsgId = msgId,
                text = text,
                timeFormatted = timeFormatted,
                timestampMs = timestampMs,
                status = MessageDeliveryStatus.DELIVERED.name
            )
        } else {
            val newConv = ConversationEntity(
                id = senderAddress,
                peerId = senderAddress,
                displayName = senderName,
                peerAddress = senderAddress,
                publicKeyFingerprint = "ed25519:" + UUID.randomUUID().toString().take(8),
                distanceHops = 1,
                rssi = -65,
                peerStatus = PeerStatus.DIRECT_NEIGHBOR.name,
                relayVia = null,
                lastSeenMinutesAgo = 0,
                isRelayCapable = true,
                lastMessageId = msgId,
                lastMessageText = text,
                lastMessageTimestampFormatted = timeFormatted,
                lastMessageTimestampMs = timestampMs,
                lastDeliveryStatus = MessageDeliveryStatus.DELIVERED.name,
                unreadCount = 1,
                isSampleDemo = false,
                isGroup = false
            )
            conversationDao.insertConversation(newConv)
        }

        return entity.toDomainModel()
    }

    /**
     * Requirement 8: Update delivery state accurately (Pending, Sent, Delivered, Failed).
     * Marked Delivered only upon valid recipient ACK.
     */
    suspend fun updateDeliveryStatus(messageId: String, status: MessageDeliveryStatus, isAckReceived: Boolean) {
        chatMessageDao.updateDeliveryStatus(messageId, status.name, isAckReceived)
        val msg = chatMessageDao.getMessageById(messageId)
        if (msg != null) {
            val conv = conversationDao.getConversationById(msg.conversationId)
            if (conv != null && conv.lastMessageId == messageId) {
                conversationDao.updateLastMessage(
                    convId = msg.conversationId,
                    lastMsgId = messageId,
                    text = msg.text,
                    timeFormatted = msg.timestampFormatted,
                    timestampMs = msg.timestampMs,
                    status = status.name
                )
            }
        }
    }

    suspend fun markMessageTransmitting(messageId: String) {
        updateDeliveryStatus(messageId, MessageDeliveryStatus.SENT, isAckReceived = false)
    }

    suspend fun markMessageDelivered(messageId: String) {
        updateDeliveryStatus(messageId, MessageDeliveryStatus.DELIVERED, isAckReceived = true)
    }

    suspend fun markMessageFailed(messageId: String, reason: String = "Delivery failed") {
        updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED, isAckReceived = false)
    }

    suspend fun getOrCreateRealConversation(peerAddress: String, peerName: String): PersonalConversation {
        val existing = conversationDao.getConversationById(peerAddress)
        if (existing != null) {
            return existing.toDomainModel()
        }

        val timestampMs = System.currentTimeMillis()
        val emptyNotice = "Tap to send direct BLE offline message"
        val convEntity = ConversationEntity(
            id = peerAddress,
            peerId = peerAddress,
            displayName = peerName,
            peerAddress = peerAddress,
            publicKeyFingerprint = "ed25519:" + UUID.randomUUID().toString().take(8),
            distanceHops = 1,
            rssi = -60,
            peerStatus = PeerStatus.DIRECT_NEIGHBOR.name,
            relayVia = null,
            lastSeenMinutesAgo = 0,
            isRelayCapable = true,
            lastMessageId = null,
            lastMessageText = emptyNotice,
            lastMessageTimestampFormatted = formatTime(timestampMs),
            lastMessageTimestampMs = timestampMs,
            lastDeliveryStatus = MessageDeliveryStatus.PENDING.name,
            unreadCount = 0,
            isSampleDemo = false,
            isGroup = false
        )
        conversationDao.insertConversation(convEntity)
        return convEntity.toDomainModel()
    }

    suspend fun initializeSampleDataIfEmpty() {
        if (conversationDao.getConversationCount() > 0) {
            return
        }

        val now = System.currentTimeMillis()

        // 1. Sarah Chen
        val sarahId = "NODE_9A4C"
        val sarahConv = ConversationEntity(
            id = sarahId,
            peerId = sarahId,
            displayName = "Sarah Chen",
            peerAddress = null,
            publicKeyFingerprint = "ed25519:7b3a..410f",
            distanceHops = 1,
            rssi = -64,
            peerStatus = PeerStatus.DIRECT_NEIGHBOR.name,
            relayVia = null,
            lastSeenMinutesAgo = 2,
            isRelayCapable = true,
            lastMessageId = "MSG_003",
            lastMessageText = "I have Marcus linked 1 hop behind me. We can relay messages through my device if he stays within 30 meters.",
            lastMessageTimestampFormatted = "10:19 AM",
            lastMessageTimestampMs = now - 600000,
            lastDeliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
            unreadCount = 0,
            isSampleDemo = true,
            isGroup = false
        )

        val sarahMessages = listOf(
            ChatMessageEntity(
                id = "MSG_001",
                conversationId = sarahId,
                senderNodeId = sarahId,
                senderName = "Sarah Chen",
                isOutgoing = false,
                text = "Cell towers appear to be completely down across the valley. Switched to MeshLink.",
                timestampFormatted = "10:14 AM",
                timestampMs = now - 900000,
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
                isAckReceived = true,
                isSampleDemo = true,
                hopsTraversed = 1,
                routePath = "$sarahId,$localNodeId"
            ),
            ChatMessageEntity(
                id = "MSG_002",
                conversationId = sarahId,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Copy that. My phone is picking up your BLE advertisement on direct RF link.",
                timestampFormatted = "10:16 AM",
                timestampMs = now - 780000,
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
                isAckReceived = true,
                isSampleDemo = true,
                hopsTraversed = 1,
                routePath = "$localNodeId,$sarahId"
            ),
            ChatMessageEntity(
                id = "MSG_003",
                conversationId = sarahId,
                senderNodeId = sarahId,
                senderName = "Sarah Chen",
                isOutgoing = false,
                text = "I have Marcus linked 1 hop behind me. We can relay messages through my device if he stays within 30 meters.",
                timestampFormatted = "10:19 AM",
                timestampMs = now - 600000,
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
                isAckReceived = true,
                isSampleDemo = true,
                hopsTraversed = 1,
                routePath = "$sarahId,$localNodeId"
            )
        )

        // 2. Marcus Vance
        val marcusId = "NODE_88B1"
        val marcusConv = ConversationEntity(
            id = marcusId,
            peerId = marcusId,
            displayName = "Marcus Vance",
            peerAddress = null,
            publicKeyFingerprint = "ed25519:3c91..99d2",
            distanceHops = 2,
            rssi = null,
            peerStatus = PeerStatus.MULTI_HOP_REACHABLE.name,
            relayVia = "NODE_9A4C (Sarah)",
            lastSeenMinutesAgo = 6,
            isRelayCapable = true,
            lastMessageId = "MSG_102",
            lastMessageText = "Received through Sarah's relay. 2 hops total, end-to-end encrypted.",
            lastMessageTimestampFormatted = "10:24 AM",
            lastMessageTimestampMs = now - 300000,
            lastDeliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
            unreadCount = 1,
            isSampleDemo = true,
            isGroup = false
        )

        val marcusMessages = listOf(
            ChatMessageEntity(
                id = "MSG_101",
                conversationId = marcusId,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Marcus, checking multi-hop packet forwarding through Sarah's node.",
                timestampFormatted = "10:22 AM",
                timestampMs = now - 420000,
                deliveryStatus = MessageDeliveryStatus.RELAYED_HOP.name,
                isAckReceived = false,
                isSampleDemo = true,
                hopsTraversed = 2,
                routePath = "$localNodeId,$sarahId,$marcusId"
            ),
            ChatMessageEntity(
                id = "MSG_102",
                conversationId = marcusId,
                senderNodeId = marcusId,
                senderName = "Marcus Vance",
                isOutgoing = false,
                text = "Received through Sarah's relay. 2 hops total, end-to-end encrypted.",
                timestampFormatted = "10:24 AM",
                timestampMs = now - 300000,
                deliveryStatus = MessageDeliveryStatus.DELIVERED_PEER.name,
                isAckReceived = true,
                isSampleDemo = true,
                hopsTraversed = 2,
                routePath = "$marcusId,$sarahId,$localNodeId"
            )
        )

        // 3. Elena Rostova
        val elenaId = "NODE_31FE"
        val elenaConv = ConversationEntity(
            id = elenaId,
            peerId = elenaId,
            displayName = "Elena Rostova",
            peerAddress = null,
            publicKeyFingerprint = "ed25519:12ff..e41a",
            distanceHops = 3,
            rssi = null,
            peerStatus = PeerStatus.ROUTE_SEARCHING.name,
            relayVia = "NODE_4B07 (Relay Outpost)",
            lastSeenMinutesAgo = 14,
            isRelayCapable = false,
            lastMessageId = "MSG_201",
            lastMessageText = "Do you have the medical supplies checklist? (Stored locally, awaiting mesh path)",
            lastMessageTimestampFormatted = "09:45 AM",
            lastMessageTimestampMs = now - 3600000,
            lastDeliveryStatus = MessageDeliveryStatus.STORED_LOCALLY.name,
            unreadCount = 0,
            isSampleDemo = true,
            isGroup = false
        )

        val elenaMessages = listOf(
            ChatMessageEntity(
                id = "MSG_201",
                conversationId = elenaId,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Do you have the medical supplies checklist? (Stored locally, awaiting mesh path)",
                timestampFormatted = "09:45 AM",
                timestampMs = now - 3600000,
                deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY.name,
                isAckReceived = false,
                isSampleDemo = true,
                hopsTraversed = 0,
                routePath = localNodeId
            )
        )

        // 4. Dr. Liam Scott
        val liamId = "NODE_04D7"
        val liamConv = ConversationEntity(
            id = liamId,
            peerId = liamId,
            displayName = "Dr. Liam Scott",
            peerAddress = null,
            publicKeyFingerprint = "ed25519:aa02..88b4",
            distanceHops = 0,
            rssi = null,
            peerStatus = PeerStatus.OUT_OF_RANGE.name,
            relayVia = null,
            lastSeenMinutesAgo = 120,
            isRelayCapable = false,
            lastMessageId = "MSG_301",
            lastMessageText = "Dr. Liam, please advise when your outpost node comes online.",
            lastMessageTimestampFormatted = "Yesterday",
            lastMessageTimestampMs = now - 86400000,
            lastDeliveryStatus = MessageDeliveryStatus.STORED_LOCALLY.name,
            unreadCount = 0,
            isSampleDemo = true,
            isGroup = false
        )

        val liamMessages = listOf(
            ChatMessageEntity(
                id = "MSG_301",
                conversationId = liamId,
                senderNodeId = localNodeId,
                senderName = "Me",
                isOutgoing = true,
                text = "Dr. Liam, please advise when your outpost node comes online.",
                timestampFormatted = "Yesterday",
                timestampMs = now - 86400000,
                deliveryStatus = MessageDeliveryStatus.STORED_LOCALLY.name,
                isAckReceived = false,
                isSampleDemo = true,
                hopsTraversed = 0,
                routePath = localNodeId
            )
        )

        conversationDao.insertConversations(listOf(sarahConv, marcusConv, elenaConv, liamConv))
        chatMessageDao.insertMessages(sarahMessages + marcusMessages + elenaMessages + liamMessages)
    }

    private fun formatTime(timestampMs: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(timestampMs))
    }

    private fun ChatMessageEntity.toDomainModel(): ChatMessage {
        val status = try {
            MessageDeliveryStatus.valueOf(deliveryStatus)
        } catch (e: Exception) {
            MessageDeliveryStatus.STORED_LOCALLY
        }

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderNodeId = senderNodeId,
            senderName = senderName,
            isOutgoing = isOutgoing,
            text = text,
            timestampFormatted = timestampFormatted,
            deliveryStatus = status,
            hopsTraversed = hopsTraversed,
            maxHops = maxHops,
            routePath = if (routePath.isEmpty()) emptyList() else routePath.split(","),
            isE2EEncrypted = isE2EEncrypted,
            estimatedPacketBytes = estimatedPacketBytes,
            timestampMs = timestampMs,
            isSampleDemo = isSampleDemo,
            isAckReceived = isAckReceived
        )
    }

    private fun ConversationEntity.toDomainModel(): PersonalConversation {
        val pStatus = try {
            PeerStatus.valueOf(peerStatus)
        } catch (e: Exception) {
            PeerStatus.DIRECT_NEIGHBOR
        }

        val dStatus = try {
            MessageDeliveryStatus.valueOf(lastDeliveryStatus)
        } catch (e: Exception) {
            MessageDeliveryStatus.STORED_LOCALLY
        }

        val peer = PeerNode(
            id = peerId,
            displayName = displayName,
            publicKeyFingerprint = publicKeyFingerprint,
            distanceHops = distanceHops,
            rssi = rssi,
            status = pStatus,
            relayVia = relayVia,
            lastSeenMinutesAgo = lastSeenMinutesAgo,
            isRelayCapable = isRelayCapable
        )

        val lastMsg = ChatMessage(
            id = lastMessageId ?: ("MSG_" + id),
            conversationId = id,
            senderNodeId = if (lastDeliveryStatus == "DELIVERED" || lastDeliveryStatus == "DELIVERED_PEER") peerId else localNodeId,
            senderName = if (lastDeliveryStatus == "DELIVERED" || lastDeliveryStatus == "DELIVERED_PEER") displayName else "Me",
            isOutgoing = lastDeliveryStatus != "DELIVERED" && lastDeliveryStatus != "DELIVERED_PEER",
            text = lastMessageText,
            timestampFormatted = lastMessageTimestampFormatted,
            deliveryStatus = dStatus,
            hopsTraversed = distanceHops,
            isSampleDemo = isSampleDemo
        )

        return PersonalConversation(
            id = id,
            peer = peer,
            lastMessage = lastMsg,
            unreadCount = unreadCount,
            isSampleDemo = isSampleDemo
        )
    }
}
