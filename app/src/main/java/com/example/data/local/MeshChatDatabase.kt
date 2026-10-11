package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeshChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: MeshChatDatabase? = null

        fun getInstance(context: Context): MeshChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeshChatDatabase::class.java,
                    "meshlink_chat_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        // For fast, isolated JVM unit and Robolectric tests
        fun createInMemory(context: Context): MeshChatDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                MeshChatDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
