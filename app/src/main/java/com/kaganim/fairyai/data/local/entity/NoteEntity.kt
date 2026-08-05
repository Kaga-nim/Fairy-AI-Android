package com.kaganim.fairyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaganim.fairyai.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long
) {
    fun toNote(): Note = Note(id, title, content, timestamp)
    
    companion object {
        fun fromNote(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            timestamp = note.timestamp
        )
    }
}
