package com.kaganim.fairyai.data.local.dao

import androidx.room.*
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM fairy_memory ORDER BY timestamp DESC")
    fun getAllMemory(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM fairy_memory ORDER BY timestamp DESC")
    suspend fun getAllMemoryList(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM fairy_memory WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM fairy_memory")
    suspend fun clearMemory()
}
