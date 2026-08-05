package com.kaganim.fairyai.di

import com.kaganim.fairyai.data.repository.NoteRepositoryImpl
import com.kaganim.fairyai.data.repository.TodoRepositoryImpl
import com.kaganim.fairyai.domain.repository.NoteRepository
import com.kaganim.fairyai.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        todoRepositoryImpl: TodoRepositoryImpl
    ): TodoRepository
}
