package com.kaganim.fairyai.data.repository

import com.kaganim.fairyai.data.local.dao.TodoDao
import com.kaganim.fairyai.data.local.entity.TodoEntity
import com.kaganim.fairyai.domain.model.Todo
import com.kaganim.fairyai.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val dao: TodoDao
) : TodoRepository {
    override fun getTodos(): Flow<List<Todo>> = dao.getTodos().map { entities ->
        entities.map { it.toTodo() }
    }

    override suspend fun insertTodo(todo: Todo) {
        dao.insertTodo(TodoEntity.fromTodo(todo))
    }

    override suspend fun deleteTodo(todo: Todo) {
        dao.deleteTodo(TodoEntity.fromTodo(todo))
    }
}
