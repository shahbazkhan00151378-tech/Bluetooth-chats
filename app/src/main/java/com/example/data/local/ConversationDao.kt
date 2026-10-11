package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestampMs DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestampMs DESC")
    suspend fun getAllConversationsList(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE isSampleDemo = 0 ORDER BY lastMessageTimestampMs DESC")
    fun getRealConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isSampleDemo = 1 ORDER BY lastMessageTimestampMs DESC")
    fun getSampleConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :convId LIMIT 1")
    suspend fun getConversationById(convId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET lastMessageId = :lastMsgId, lastMessageText = :text, lastMessageTimestampFormatted = :timeFormatted, lastMessageTimestampMs = :timestampMs, lastDeliveryStatus = :status WHERE id = :convId")
    suspend fun updateLastMessage(
        convId: String,
        lastMsgId: String,
        text: String,
        timeFormatted: String,
        timestampMs: Long,
        status: String
    )

    @Query("DELETE FROM conversations WHERE id = :convId")
    suspend fun deleteConversation(convId: String)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
}
