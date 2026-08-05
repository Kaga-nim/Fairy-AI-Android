package com.kaganim.fairyai.domain.repository

import com.kaganim.fairyai.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<List<Todo>>
    suspend fun insertTodo(todo: Todo)
    suspend fun deleteTodo(todo: Todo)
}
