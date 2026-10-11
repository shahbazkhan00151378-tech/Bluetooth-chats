package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MeshChatRepository
import com.example.data.local.MeshChatDatabase
import com.example.model.MessageDeliveryStatus
import com.example.viewmodel.MeshViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatPersistenceRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MeshChatDatabase
    private lateinit var repository: MeshChatRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = MeshChatDatabase.createInMemory(context)
        repository = MeshChatRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveOutgoingMessageStartsPending() = runBlocking {
        val convId = "PEER_OFFLINE_01"
        val text = "Hello offline mesh peer"

        val message = repository.saveOutgoingMessage(
            conversationId = convId,
            text = text,
            isGroup = false
        )

        assertNotNull(message.id)
        assertEquals(convId, message.conversationId)
        assertEquals(text, message.text)
        assertTrue(message.isOutgoing)
        assertFalse(message.isAckReceived)
        assertEquals(MessageDeliveryStatus.PENDING, message.deliveryStatus)

        // Verify loaded from database
        val loaded = repository.getMessagesSnapshot(convId)
        assertEquals(1, loaded.size)
        assertEquals(message.id, loaded[0].id)
        assertEquals(MessageDeliveryStatus.PENDING, loaded[0].deliveryStatus)
    }

    @Test
    fun testDeliveryStatusTransitionToSentAndDelivered() = runBlocking {
        val convId = "PEER_BLE_02"
        val message = repository.saveOutgoingMessage(convId, "Testing BLE ACK", false)

        // Transmitting -> SENT
        repository.markMessageTransmitting(message.id)
        var snapshot = repository.getMessagesSnapshot(convId)
        assertEquals(MessageDeliveryStatus.SENT, snapshot[0].deliveryStatus)
        assertFalse(snapshot[0].isAckReceived)

        // Valid Recipient ACK received -> DELIVERED
        repository.markMessageDelivered(message.id)
        snapshot = repository.getMessagesSnapshot(convId)
        assertEquals(MessageDeliveryStatus.DELIVERED, snapshot[0].deliveryStatus)
        assertTrue(snapshot[0].isAckReceived)
    }

    @Test
    fun testDeliveryStatusTransitionToFailed() = runBlocking {
        val convId = "PEER_BLE_03"
        val message = repository.saveOutgoingMessage(convId, "Failing transmission", false)

        repository.markMessageFailed(message.id, "GATT connection timeout")
        val snapshot = repository.getMessagesSnapshot(convId)
        assertEquals(MessageDeliveryStatus.FAILED, snapshot[0].deliveryStatus)
        assertFalse(snapshot[0].isAckReceived)
    }

    @Test
    fun testSaveIncomingBleMessage() = runBlocking {
        val senderAddr = "DC:2C:26:A1:B2:C3"
        val senderName = "Nearby Rescue Phone"
        val incomingText = "Ping reply over BLE radio"

        val msg = repository.saveIncomingBleMessage(
            senderAddress = senderAddr,
            senderName = senderName,
            text = incomingText
        )

        assertFalse(msg.isOutgoing)
        assertEquals(senderAddr, msg.senderNodeId)
        assertEquals(senderName, msg.senderName)
        assertEquals(MessageDeliveryStatus.DELIVERED, msg.deliveryStatus)
        assertTrue(msg.isAckReceived)

        val conv = repository.getConversationById(senderAddr)
        assertNotNull(conv)
        assertEquals(senderName, conv?.peer?.displayName)
        assertFalse(conv?.isSampleDemo ?: true)
    }

    @Test
    fun testPersistenceAcrossRepositoryRecreation() = runBlocking {
        val convId = "PEER_PERSISTENCE_TEST"
        val text = "Message to survive app restart"

        val saved = repository.saveOutgoingMessage(convId, text, false)
        repository.markMessageDelivered(saved.id)

        // Recreate repository from the same database instance
        val newRepoInstance = MeshChatRepository(database)
        val messages = newRepoInstance.getMessagesSnapshot(convId)

        assertEquals(1, messages.size)
        assertEquals(saved.id, messages[0].id)
        assertEquals(text, messages[0].text)
        assertEquals(MessageDeliveryStatus.DELIVERED, messages[0].deliveryStatus)
        assertTrue(messages[0].isAckReceived)
    }

    @Test
    fun testSeparationOfRealAndSampleConversations() = runBlocking {
        repository.initializeSampleDataIfEmpty()

        val sampleConvs = repository.sampleConversations.first()
        assertTrue("Expected 4 mock conversations", sampleConvs.size >= 4)
        assertTrue("All sample conversations should have isSampleDemo = true", sampleConvs.all { it.isSampleDemo })

        // Create a real conversation
        repository.saveOutgoingMessage("AA:BB:CC:DD:EE:FF", "Real hardware message", false)

        val realConvs = repository.realConversations.first()
        assertEquals(1, realConvs.size)
        assertFalse(realConvs[0].isSampleDemo)
        assertEquals("AA:BB:CC:DD:EE:FF", realConvs[0].id)
    }

    @Test
    fun testViewModelMessageSendingFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MeshViewModel(app)

        val sent = viewModel.sendMessage("NODE_9A4C", "Test message via ViewModel", false)
        assertEquals("Test message via ViewModel", sent.text)
        assertTrue(sent.isOutgoing)
        assertEquals(MessageDeliveryStatus.PENDING, sent.deliveryStatus)

        val messages = viewModel.getMessagesForConversation("NODE_9A4C")
        assertTrue(messages.isNotEmpty())
        val matching = messages.find { it.text == "Test message via ViewModel" }
        assertNotNull("Expected message to be saved and found in conversation", matching)
        assertTrue(matching!!.isOutgoing)
    }
}
