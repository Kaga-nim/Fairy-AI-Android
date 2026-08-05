package com.kaganim.fairyai.data.local.dao

import androidx.room.*
import com.kaganim.fairyai.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY timestamp DESC")
    fun getTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)
}
