package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BleHardwareState
import com.example.ble.BleMeshManager
import com.example.ble.BleMessagePersistenceListener
import com.example.ble.DiscoveredPeerDevice
import com.example.ble.NativeBleMessage
import com.example.data.MockMeshRepository
import com.example.model.ChatMessage
import com.example.model.GroupChannel
import com.example.model.MessageDeliveryStatus
import com.example.model.MeshPacketFrame
import com.example.model.MeshRouteEntry
import com.example.model.PeerNode
import com.example.model.PersonalConversation
import com.example.model.TechnicalSpecItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeSection(val title: String) {
    PERSONAL_CHATS("Chats"),
    NEARBY_GROUPS("Groups"),
    MESH_NETWORK("Network")
}

data class MeshUiState(
    val currentSection: HomeSection = HomeSection.PERSONAL_CHATS,
    val selectedPersonalChatId: String? = null,
    val selectedGroupId: String? = null,
    val inspectedPacket: MeshPacketFrame? = null,
    val showDiagnosticsScreen: Boolean = false,
    val showNewPairDialog: Boolean = false,
    val lastSentDraftNotice: String? = null,
    val selectedBlePeer: DiscoveredPeerDevice? = null
)

class MeshViewModel(
    application: Application,
    val repository: MockMeshRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, MockMeshRepository(application.applicationContext))

    // Native BLE Hardware Subsystem
    val bleManager = BleMeshManager(application.applicationContext)

    val bleHardwareState: StateFlow<BleHardwareState> = bleManager.hardwareState
    val isBleAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    val isBleScanning: StateFlow<Boolean> = bleManager.isScanning
    val discoveredBlePeers: StateFlow<List<DiscoveredPeerDevice>> = bleManager.discoveredPeers
    val connectedPeerAddress: StateFlow<String?> = bleManager.connectedPeerAddress
    val peerConnectionStates: StateFlow<Map<String, com.example.ble.PeerConnectionInfo>> = bleManager.peerConnectionStates
    val nativeBleMessages: StateFlow<List<NativeBleMessage>> = bleManager.nativeMessages
    val bleLastError: StateFlow<String?> = bleManager.lastError
    val bleStatusLog: StateFlow<String?> = bleManager.statusLog

    private val _uiState = MutableStateFlow(MeshUiState())
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<PersonalConversation>> = repository.conversations
    val groupChannels: StateFlow<List<GroupChannel>> = repository.groupChannels
    val routeTable: StateFlow<List<MeshRouteEntry>> = repository.routeTable
    val discoveredNodes: StateFlow<List<PeerNode>> = repository.discoveredNodes
    val technicalSpecs: List<TechnicalSpecItem> = repository.getTechnicalSpecifications()

    init {
        // Wire BLE hardware manager to SQLite persistence layer
        bleManager.persistenceListener = object : BleMessagePersistenceListener {
            override fun onOutgoingMessageSaved(messageId: String, peerAddress: String, text: String) {
                viewModelScope.launch {
                    repository.saveOutgoingPersistentMessage(
                        convId = peerAddress,
                        text = text,
                        isGroup = false,
                        customId = messageId
                    )
                }
            }

            override fun onOutgoingMessageTransmitting(messageId: String) {
                viewModelScope.launch {
                    repository.updateDeliveryStatus(
                        messageId = messageId,
                        convId = null,
                        status = MessageDeliveryStatus.SENT,
                        isAckReceived = false
                    )
                }
            }

            override fun onOutgoingMessageDelivered(messageId: String) {
                viewModelScope.launch {
                    repository.updateDeliveryStatus(
                        messageId = messageId,
                        convId = null,
                        status = MessageDeliveryStatus.DELIVERED,
                        isAckReceived = true
                    )
                }
            }

            override fun onOutgoingMessageFailed(messageId: String, reason: String) {
                viewModelScope.launch {
                    repository.updateDeliveryStatus(
                        messageId = messageId,
                        convId = null,
                        status = MessageDeliveryStatus.FAILED,
                        isAckReceived = false
                    )
                }
            }

            override fun onIncomingMessageReceived(
                messageId: String,
                senderAddress: String,
                senderName: String,
                text: String
            ) {
                viewModelScope.launch {
                    repository.saveIncomingBleMessage(
                        senderAddr = senderAddress,
                        senderName = senderName,
                        text = text,
                        customId = messageId
                    )
                }
            }
        }
    }

    fun selectSection(section: HomeSection) {
        _uiState.value = _uiState.value.copy(
            currentSection = section,
            selectedPersonalChatId = null,
            selectedGroupId = null
        )
    }

    fun openPersonalChat(chatId: String) {
        _uiState.value = _uiState.value.copy(
            selectedPersonalChatId = chatId,
            selectedGroupId = null
        )
    }

    fun openGroupChat(groupId: String) {
        _uiState.value = _uiState.value.copy(
            selectedGroupId = groupId,
            selectedPersonalChatId = null
        )
    }

    fun closeActiveChat() {
        _uiState.value = _uiState.value.copy(
            selectedPersonalChatId = null,
            selectedGroupId = null
        )
    }

    fun openDiagnostics() {
        _uiState.value = _uiState.value.copy(showDiagnosticsScreen = true)
    }

    fun closeDiagnostics() {
        _uiState.value = _uiState.value.copy(showDiagnosticsScreen = false)
    }

    fun inspectMessagePacket(message: ChatMessage) {
        val packet = repository.generatePacketFrame(message)
        _uiState.value = _uiState.value.copy(inspectedPacket = packet)
    }

    fun dismissPacketInspection() {
        _uiState.value = _uiState.value.copy(inspectedPacket = null)
    }

    fun openNewPairDialog() {
        _uiState.value = _uiState.value.copy(showNewPairDialog = true)
    }

    fun closeNewPairDialog() {
        _uiState.value = _uiState.value.copy(showNewPairDialog = false)
    }

    fun sendMessage(convId: String, text: String, isGroup: Boolean = false): ChatMessage {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val timeStr = formatter.format(java.util.Date())
        val msgId = "TX_" + java.util.UUID.randomUUID().toString().take(6)

        val localMsg = ChatMessage(
            id = msgId,
            conversationId = convId,
            senderNodeId = repository.localNodeId,
            senderName = "Me",
            isOutgoing = true,
            text = text,
            timestampFormatted = timeStr,
            deliveryStatus = MessageDeliveryStatus.PENDING,
            hopsTraversed = 0,
            maxHops = if (isGroup) 4 else 3,
            routePath = listOf(repository.localNodeId),
            isE2EEncrypted = !isGroup,
            estimatedPacketBytes = text.toByteArray().size + 42
        )
        repository.addLocalMessage(localMsg)

        viewModelScope.launch {
            // Persist to Room SQLite
            repository.saveOutgoingPersistentMessage(convId, text, isGroup, msgId)

            // Check if there is an active matching BLE peer for direct transmission
            val matchingPeer = discoveredBlePeers.value.find { it.address.equals(convId, ignoreCase = true) }
            if (matchingPeer != null) {
                bleManager.sendDirectBleMessage(matchingPeer.device, text, msgId)
                _uiState.value = _uiState.value.copy(
                    lastSentDraftNotice = "Saved locally. Transmitting via direct BLE..."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    lastSentDraftNotice = "Saved locally (Room/SQLite) - Awaiting RF link."
                )
            }
        }

        return localMsg
    }

    fun sendLocalDraft(convId: String, text: String, isGroup: Boolean) {
        sendMessage(convId, text, isGroup)
    }

    fun startChatWithBlePeer(peer: DiscoveredPeerDevice) {
        viewModelScope.launch {
            val conv = repository.getOrCreateRealConversation(peer.address, peer.name)
            openPersonalChat(conv.id)
        }
    }

    fun getMessagesForConversation(convId: String): List<ChatMessage> {
        return repository.getMessagesForConversation(convId)
    }

    fun getMessagesFlow(convId: String): Flow<List<ChatMessage>> {
        return repository.getMessagesFlow(convId)
    }

    fun loadMessages(convId: String) {
        viewModelScope.launch {
            repository.loadMessagesForConversation(convId)
        }
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(lastSentDraftNotice = null)
    }

    // Native BLE Operations
    fun isBleSupported(): Boolean = bleManager.isBleSupported()
    fun isBluetoothEnabled(): Boolean = bleManager.isBluetoothEnabled()
    fun hasBlePermissions(): Boolean = bleManager.hasRequiredPermissions()
    fun onPermissionsDenied() = bleManager.onPermissionDenied()

    fun startBleAdvertising() {
        bleManager.startAdvertising()
    }

    fun stopBleAdvertising() {
        bleManager.stopAdvertising()
    }

    fun startBleScanning() {
        bleManager.startScanning()
    }

    fun stopBleScanning() {
        bleManager.stopScanning()
    }

    fun sendNativeBlePing(device: BluetoothDevice, text: String) {
        bleManager.sendDirectBleMessage(device, text)
    }

    fun disconnectBlePeer(address: String? = null) {
        bleManager.disconnectPeer(address)
    }

    fun selectBlePeer(peer: DiscoveredPeerDevice?) {
        _uiState.value = _uiState.value.copy(selectedBlePeer = peer)
    }

    fun refreshBleState() {
        bleManager.refreshHardwareState()
    }

    fun clearBleError() {
        bleManager.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
    }
}
