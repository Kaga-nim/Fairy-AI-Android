package com.kaganim.fairyai.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaganim.fairyai.data.local.dao.ChatDao
import com.kaganim.fairyai.data.local.dao.MemoryDao
import com.kaganim.fairyai.data.local.dao.NoteDao
import com.kaganim.fairyai.data.local.dao.TodoDao
import com.kaganim.fairyai.data.local.entity.ChatEntity
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import com.kaganim.fairyai.data.local.entity.NoteEntity
import com.kaganim.fairyai.data.local.entity.TodoEntity

@Database(
    entities = [NoteEntity::class, TodoEntity::class, MemoryEntity::class, ChatEntity::class],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val todoDao: TodoDao
    abstract val memoryDao: MemoryDao
    abstract val chatDao: ChatDao

    companion object {
        // Combined manual migration to reach version 13 from any state between 5 and 12
        val MIGRATION_TO_13 = object : Migration(5, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateAllTables(db)
            }
        }

        // Specific paths to ensure Room finds the migration
        val MIGRATIONS = arrayOf(
            object : Migration(5, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(6, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(7, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(8, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(9, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(10, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(11, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) },
            object : Migration(12, 13) { override fun migrate(db: SupportSQLiteDatabase) = recreateAllTables(db) }
        )

        private fun recreateAllTables(db: SupportSQLiteDatabase) {
            // We only really need to fix fairy_memory, but let's be safe
            
            // 1. Fix fairy_memory
            db.execSQL("CREATE TABLE IF NOT EXISTS `fairy_memory_fix` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, `category` TEXT NOT NULL DEFAULT 'PERSONAL', `timestamp` INTEGER NOT NULL)")
            
            try {
                val cursor = db.query("PRAGMA table_info(fairy_memory)")
                val cols = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()
                
                if ("category" in cols) {
                    db.execSQL("INSERT INTO `fairy_memory_fix` (id, `key`, value, category, timestamp) SELECT id, `key`, value, category, timestamp FROM fairy_memory")
                } else {
                    db.execSQL("INSERT INTO `fairy_memory_fix` (id, `key`, value, timestamp) SELECT id, `key`, value, timestamp FROM fairy_memory")
                }
            } catch (e: Exception) {}
            
            db.execSQL("DROP TABLE IF EXISTS `fairy_memory`")
            db.execSQL("ALTER TABLE `fairy_memory_fix` RENAME TO `fairy_memory`")
            
            // 2. Ensure chat_messages has imageUri (added in v5)
            try {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `imageUri` TEXT")
            } catch (e: Exception) {}
        }
    }
}
