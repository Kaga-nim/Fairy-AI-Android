package com.kaganim.fairyai.domain.usecase

import com.kaganim.fairyai.domain.model.Note
import com.kaganim.fairyai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class NoteUseCases(
    val getNotes: GetNotes,
    val deleteNote: DeleteNote,
    val addNote: AddNote,
    val getNote: GetNote
)

class GetNotes @Inject constructor(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repository.getNotes()
}

class DeleteNote @Inject constructor(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) = repository.deleteNote(note)
}

class AddNote @Inject constructor(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) = repository.insertNote(note)
}

class GetNote @Inject constructor(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Int): Note? = repository.getNoteById(id)
}
