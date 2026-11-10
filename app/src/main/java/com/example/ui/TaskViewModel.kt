package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TodoDao
import com.example.data.toEntity
import com.example.data.toTask
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Success(val tasks: List<Task>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

/**
 * TaskViewModel handling the business logic of adding, deleting, and fetching
 * tasks from the Room database and scheduling AlarmManager notifications.
 */
class TaskViewModel(
    application: Application,
    private val todoDao: TodoDao = AppDatabase.getDatabase(application).todoDao()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Observable stream of tasks from the database mapped to the Task data class
    val tasks: StateFlow<List<Task>> = todoDao.getAllTodosFlow()
        .map { entities ->
            entities.map { it.toTask() }
        }
        .catch { error ->
            _uiState.value = TaskUiState.Error(error.localizedMessage ?: "Unknown error")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /**
     * Filtered stream of tasks matching the search query (title or content/notes) and category.
     */
    val filteredTasks: StateFlow<List<Task>> = kotlinx.coroutines.flow.combine(
        tasks,
        _searchQuery,
        _selectedCategory
    ) { taskList, query, category ->
        taskList.filter { task ->
            val matchesCategory = category == null || task.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.notes.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        fetchTasks()
    }

    /**
     * Fetches all tasks using coroutines and updates the UI state.
     */
    fun fetchTasks(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = TaskUiState.Loading
            try {
                todoDao.getAllTodosFlow().collect { entities ->
                    val taskList = entities.map { it.toTask() }
                    _uiState.value = TaskUiState.Success(taskList)
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.localizedMessage ?: "Failed to fetch tasks")
            }
        }
    }

    /**
     * Adds a new task to the database and schedules an AlarmManager notification
     * at the specific due time if defined.
     */
    fun addTask(
        title: String,
        category: String = "General",
        dueTime: Long? = null,
        repeatInterval: String = "NONE",
        notes: String = "",
        priority: Int = 1
    ): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            val task = Task(
                title = title.trim(),
                category = category.trim(),
                dueTime = dueTime,
                repeatInterval = repeatInterval,
                notes = notes.trim(),
                priority = priority,
                isCompleted = false
            )
            val newId = todoDao.insertTodo(task.toEntity())
            val savedTask = task.copy(id = newId)

            // Trigger AlarmManager notification at the specific due time
            if (dueTime != null && dueTime > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTask(getApplication(), savedTask)
            }
        }
    }

    /**
     * Deletes a task from the database and cancels its AlarmManager alarm.
     */
    fun deleteTask(task: Task): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.cancelReminder(getApplication(), task.id)
            todoDao.deleteTodo(task.toEntity())
        }
    }

    /**
     * Deletes a task by its ID and cancels its AlarmManager alarm.
     */
    fun deleteTaskById(taskId: Long): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.cancelReminder(getApplication(), taskId)
            todoDao.deleteTodoById(taskId)
        }
    }

    /**
     * Toggles the completion status of a task and manages the AlarmManager schedule.
     */
    fun toggleTaskStatus(task: Task): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            val updatedStatus = !task.isCompleted
            val completedTime = if (updatedStatus) System.currentTimeMillis() else null
            todoDao.setCompleted(task.id, updatedStatus, completedTime)

            if (updatedStatus) {
                // Cancel scheduled notification if completed
                ReminderScheduler.cancelReminder(getApplication(), task.id)
            } else if (task.dueTime != null && task.dueTime > System.currentTimeMillis()) {
                // Reschedule alarm if marked incomplete
                ReminderScheduler.scheduleTask(getApplication(), task)
            }
        }
    }

    /**
     * Retrieves a task by its ID.
     */
    suspend fun getTaskById(taskId: Long): Task? {
        return todoDao.getTodoById(taskId)?.toTask()
    }

    /**
     * Filters tasks by a specific category.
     */
    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }
}
