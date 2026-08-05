package com.kaganim.fairyai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kaganim.fairyai.data.local.dao.MemoryDao
import com.kaganim.fairyai.data.local.dao.NoteDao
import com.kaganim.fairyai.data.local.dao.TodoDao
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import com.kaganim.fairyai.data.local.entity.NoteEntity
import com.kaganim.fairyai.data.local.entity.TodoEntity

@Database(entities = [NoteEntity::class, TodoEntity::class, MemoryEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val todoDao: TodoDao
    abstract val memoryDao: MemoryDao
}
