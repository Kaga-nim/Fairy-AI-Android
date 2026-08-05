package id.kaganim.fairyai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Add DAOs here
}
