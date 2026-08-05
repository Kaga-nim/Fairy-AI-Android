package com.kaganim.fairyai.data.repository

import com.kaganim.fairyai.data.local.dao.NoteDao
import com.kaganim.fairyai.data.local.entity.NoteEntity
import com.kaganim.fairyai.domain.model.Note
import com.kaganim.fairyai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> {
        return dao.getNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override suspend fun getNoteById(id: Int): Note? {
        return dao.getNoteById(id)?.toNote()
    }

    override suspend fun insertNote(note: Note) {
        dao.insertNote(NoteEntity.fromNote(note))
    }

    override suspend fun deleteNote(note: Note) {
        dao.deleteNote(NoteEntity.fromNote(note))
    }
}
