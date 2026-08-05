package com.kaganim.fairyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fairy_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
