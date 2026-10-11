package com.example

import com.example.ble.BleConstants
import com.example.ble.BleHardwareState
import com.example.ble.NativeBleMessage
import com.example.data.MockMeshRepository
import com.example.model.MessageDeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMockMeshRepositoryInitialization() {
        val repo = MockMeshRepository()
        assertEquals(4, repo.conversations.value.size)
        assertEquals(3, repo.groupChannels.value.size)
        assertEquals(4, repo.routeTable.value.size)
        assertEquals(4, repo.discoveredNodes.value.size)
        assertTrue(repo.getTechnicalSpecifications().isNotEmpty())
    }

    @Test
    fun testSendLocalDraftMessageStoresOffline() {
        val repo = MockMeshRepository()
        val draft = repo.sendLocalDraftMessage(
            convId = "NODE_9A4C",
            text = "Test local packet draft",
            isGroup = false
        )

        assertNotNull(draft)
        assertEquals(MessageDeliveryStatus.STORED_LOCALLY, draft.deliveryStatus)
        assertEquals(0, draft.hopsTraversed)
        assertTrue(draft.isE2EEncrypted)

        val packet = repo.generatePacketFrame(draft)
        assertEquals("P2P_UNICAST_DATA", packet.packetType)
        assertEquals("NODE_F8A2_LOCAL", packet.sourceNode)
        assertEquals("NODE_9A4C", packet.destinationNode)
    }

    @Test
    fun testGroupBroadcastDraftPacket() {
        val repo = MockMeshRepository()
        val draft = repo.sendLocalDraftMessage(
            convId = "GRP_EMERGENCY",
            text = "Test emergency broadcast",
            isGroup = true
        )

        val packet = repo.generatePacketFrame(draft)
        assertEquals("MESH_BROADCAST_DATA", packet.packetType)
    }

    @Test
    fun testBleConstantsAndUuidDefinitions() {
        assertEquals("0000fd42-0000-1000-8000-00805f9b34fb", BleConstants.SERVICE_UUID.toString().lowercase())
        assertEquals("0000fd43-0000-1000-8000-00805f9b34fb", BleConstants.CHAR_MESSAGE_UUID.toString().lowercase())
        assertEquals(20, BleConstants.DEFAULT_MAX_PAYLOAD_BYTES)
    }

    @Test
    fun testNativeBleMessageDataModel() {
        val msg = NativeBleMessage(
            id = "TX_01",
            senderAddress = "DC:2C:26:01:02:03",
            senderName = "Peer Phone",
            text = "PING",
            timestamp = "12:00:00 PM",
            isOutgoing = true,
            deliveryStatus = "Delivered (Direct BLE ACK)",
            isAckReceived = true
        )

        assertEquals("PING", msg.text)
        assertTrue(msg.isOutgoing)
        assertTrue(msg.isAckReceived)
    }
}
