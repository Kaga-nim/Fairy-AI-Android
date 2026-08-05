package com.kaganim.fairyai.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity
data class DummyEntity(@PrimaryKey val id: Int)

@Database(entities = [DummyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Add DAOs here
}
