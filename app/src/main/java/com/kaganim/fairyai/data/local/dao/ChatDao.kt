package com.kaganim.fairyai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kaganim.fairyai.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_messages WHERE timestamp < :threshold")
    suspend fun deleteOldMessages(threshold: Long)
}
