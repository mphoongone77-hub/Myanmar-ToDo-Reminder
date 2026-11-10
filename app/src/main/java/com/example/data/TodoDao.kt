package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, dueDateMillis ASC, createdAtMillis DESC")
    fun getAllTodosFlow(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: Long): TodoEntity?

    @Query("SELECT * FROM todos WHERE category = :category ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getTodosByCategory(category: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND dueDateMillis <= :dueTimeMillis ORDER BY dueDateMillis ASC")
    fun getPendingTodosDueBefore(dueTimeMillis: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND reminderTimeMillis > :currentTimeMillis")
    suspend fun getUpcomingReminders(currentTimeMillis: Long): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, dueDateMillis ASC, createdAtMillis DESC")
    suspend fun getPendingTodos(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, dueDateMillis ASC, createdAtMillis DESC")
    fun getPendingTodosSync(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun clearCompleted()
}
