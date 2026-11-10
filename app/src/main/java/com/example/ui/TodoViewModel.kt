package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.TodoEntity
import com.example.reminder.ReminderScheduler
import com.example.voice.VoiceManager
import com.example.util.NetworkConnectivityObserver
import com.example.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FilterTab(val titleMm: String, val titleEn: String) {
    ALL("အားလုံး", "All"),
    TODAY("ဒီကနေ့", "Today"),
    UPCOMING("လာမည့်ရက်", "Upcoming"),
    COMPLETED("ပြီးစီးပြီး", "Completed")
}

data class TodoStats(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val completionRate: Float = 0f
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val todoDao = db.todoDao()
    private val categoryDao = db.categoryDao()
    val voiceManager = VoiceManager(application)
    private val connectivityObserver = NetworkConnectivityObserver(application)

    // Dynamic Categories from Room DB
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isCategoryManagerOpen = MutableStateFlow(false)
    val isCategoryManagerOpen: StateFlow<Boolean> = _isCategoryManagerOpen.asStateFlow()

    // Offline / Online Connectivity Status
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = connectivityObserver.isCurrentlyOnline()
    )

    private val _isOfflineInfoSheetOpen = MutableStateFlow(false)
    val isOfflineInfoSheetOpen: StateFlow<Boolean> = _isOfflineInfoSheetOpen.asStateFlow()

    private val _selectedTab = MutableStateFlow(FilterTab.ALL)
    val selectedTab: StateFlow<FilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isMyanmarLanguage = MutableStateFlow(true)
    val isMyanmarLanguage: StateFlow<Boolean> = _isMyanmarLanguage.asStateFlow()

    // Editing / Add Dialog State
    private val _activeEditingTodo = MutableStateFlow<TodoEntity?>(null)
    val activeEditingTodo: StateFlow<TodoEntity?> = _activeEditingTodo.asStateFlow()

    private val _isAddEditModalOpen = MutableStateFlow(false)
    val isAddEditModalOpen: StateFlow<Boolean> = _isAddEditModalOpen.asStateFlow()

    // Voice Input Sheet State
    private val _isVoiceInputSheetOpen = MutableStateFlow(false)
    val isVoiceInputSheetOpen: StateFlow<Boolean> = _isVoiceInputSheetOpen.asStateFlow()

    // User Feedback Toast / SnackBar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Raw Todos from DB
    val allTodos = todoDao.getAllTodosFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered Todos
    val filteredTodos = combine(
        allTodos,
        _selectedTab,
        _searchQuery,
        _selectedCategory
    ) { todos, tab, query, category ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfToday = cal.timeInMillis

        todos.filter { todo ->
            // Category filter
            val matchesCategory = category == null || todo.category == category

            // Search query filter (matches title or notes)
            val matchesQuery = query.isBlank() ||
                todo.title.contains(query, ignoreCase = true) ||
                todo.notes.contains(query, ignoreCase = true)

            // Tab filter
            val matchesTab = when (tab) {
                FilterTab.ALL -> true
                FilterTab.TODAY -> {
                    val due = todo.dueDateMillis
                    val reminder = todo.reminderTimeMillis
                    (due != null && due in startOfToday..endOfToday) ||
                    (reminder != null && reminder in startOfToday..endOfToday) ||
                    (todo.createdAtMillis in startOfToday..endOfToday && !todo.isCompleted)
                }
                FilterTab.UPCOMING -> {
                    val due = todo.dueDateMillis ?: todo.reminderTimeMillis
                    !todo.isCompleted && due != null && due > endOfToday
                }
                FilterTab.COMPLETED -> todo.isCompleted
            }

            matchesCategory && matchesQuery && matchesTab
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats = allTodos.combine(_searchQuery) { todos, _ ->
        val total = todos.size
        val completed = todos.count { it.isCompleted }
        val pending = total - completed
        val rate = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        TodoStats(total, pending, completed, rate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoStats()
    )

    init {
        // Pre-populate default categories if empty
        viewModelScope.launch {
            if (categoryDao.getCategoryCount() == 0) {
                seedDefaultCategories()
            }
        }

        // Pre-populate initial tasks if database is empty so user gets an instant Myanmar experience
        viewModelScope.launch {
            allTodos.collect { list ->
                if (list.isEmpty()) {
                    seedInitialTasks()
                }
            }
        }
    }

    private suspend fun seedDefaultCategories() {
        val defaults = listOf(
            CategoryEntity(name = "General", nameMm = "အထွေထွေ", isDefault = true),
            CategoryEntity(name = "Work", nameMm = "အလုပ်", isDefault = true),
            CategoryEntity(name = "Personal", nameMm = "ကိုယ်ရေး", isDefault = true),
            CategoryEntity(name = "Shopping", nameMm = "ဈေးဝယ်", isDefault = true),
            CategoryEntity(name = "Health", nameMm = "ကျန်းမာရေး", isDefault = true),
            CategoryEntity(name = "Study", nameMm = "ပညာရေး", isDefault = true)
        )
        for (item in defaults) {
            categoryDao.insertCategory(item)
        }
    }

    private suspend fun seedInitialTasks() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 2)
        val reminder1 = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val reminder2 = cal.timeInMillis

        val samples = listOf(
            TodoEntity(
                title = "မြန်မာလို အသံဖြင့် အလုပ်အသစ် ထည့်ကြည့်ပါ",
                notes = "အောက်ခြေရှိ မိုက်ကရိုဖုန်းခလုတ် (🎤) ကို နှိပ်ပြီး မိမိလုပ်ဆောင်လိုသည်ကို မြန်မာလို အသံထွက်ပြောဆိုနိုင်ပါသည်",
                category = "အထွေထွေ",
                priority = 2,
                reminderTimeMillis = reminder1
            ),
            TodoEntity(
                title = "ကျန်းမာရေးအတွက် ရေ ၂ လီတာ သောက်ရန်",
                notes = "တစ်နေ့တာလုံး အချိန်မှန် ရေသောက်ပေးရန် သတိပေးချက်",
                category = "ကျန်းမာရေး",
                priority = 1,
                reminderTimeMillis = reminder2
            ),
            TodoEntity(
                title = "ညနေ ဈေးဝယ်ထွက်ရန် စာရင်းပြုစုခြင်း",
                notes = "ဟင်းသီးဟင်းရွက်၊ သစ်သီးနှင့် နို့ထွက်ပစ္စည်းများ ဝယ်ယူရန်",
                category = "ဈေးဝယ်",
                priority = 1
            )
        )

        for (sample in samples) {
            val id = todoDao.insertTodo(sample)
            if (sample.reminderTimeMillis != null) {
                ReminderScheduler.scheduleReminder(getApplication(), sample.copy(id = id))
            }
        }
    }

    fun setFilterTab(tab: FilterTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleLanguage() {
        _isMyanmarLanguage.value = !_isMyanmarLanguage.value
    }

    fun openAddModal(todo: TodoEntity? = null) {
        _activeEditingTodo.value = todo
        _isAddEditModalOpen.value = true
    }

    fun closeAddModal() {
        _isAddEditModalOpen.value = false
        _activeEditingTodo.value = null
    }

    fun openVoiceInputSheet() {
        _isVoiceInputSheetOpen.value = true
        voiceManager.startListening()
    }

    fun closeVoiceInputSheet() {
        voiceManager.stopListening()
        voiceManager.resetSpeechState()
        _isVoiceInputSheetOpen.value = false
    }

    fun openOfflineInfo() {
        _isOfflineInfoSheetOpen.value = true
    }

    fun closeOfflineInfo() {
        _isOfflineInfoSheetOpen.value = false
    }

    fun openCategoryManager() {
        _isCategoryManagerOpen.value = true
    }

    fun closeCategoryManager() {
        _isCategoryManagerOpen.value = false
    }

    fun addCustomCategory(name: String, nameMm: String = name) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            categoryDao.insertCategory(
                CategoryEntity(
                    name = trimmed,
                    nameMm = nameMm.trim().ifBlank { trimmed },
                    isDefault = false
                )
            )
            _snackbarMessage.value = if (_isMyanmarLanguage.value)
                "အမျိုးအစားအသစ် '$trimmed' ထည့်ပြီးပါပြီ"
            else "Category '$trimmed' added"
        }
    }

    fun deleteCustomCategory(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
            if (_selectedCategory.value == category.name || _selectedCategory.value == category.nameMm) {
                _selectedCategory.value = null
            }
            _snackbarMessage.value = if (_isMyanmarLanguage.value)
                "အမျိုးအစား ဖျက်လိုက်ပါပြီ"
            else "Category deleted"
        }
    }

    fun saveTodo(
        id: Long = 0,
        title: String,
        notes: String,
        category: String,
        priority: Int,
        dueDateMillis: Long?,
        reminderTimeMillis: Long?,
        repeatInterval: String,
        voiceNotePath: String?
    ) {
        viewModelScope.launch {
            val todo = TodoEntity(
                id = id,
                title = title.trim(),
                notes = notes.trim(),
                category = category,
                priority = priority,
                dueDateMillis = dueDateMillis,
                reminderTimeMillis = reminderTimeMillis,
                repeatInterval = repeatInterval,
                voiceNotePath = voiceNotePath,
                createdAtMillis = if (id != 0L) {
                    _activeEditingTodo.value?.createdAtMillis ?: System.currentTimeMillis()
                } else System.currentTimeMillis()
            )

            val finalId = if (id == 0L) {
                todoDao.insertTodo(todo)
            } else {
                todoDao.updateTodo(todo)
                id
            }

            // Schedule or cancel reminder
            if (reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(getApplication(), todo.copy(id = finalId))
                _snackbarMessage.value = if (_isMyanmarLanguage.value)
                    "⏰ သတိပေးချက် သတ်မှတ်ပြီးပါပြီ"
                else "⏰ Reminder scheduled"
            } else {
                ReminderScheduler.cancelReminder(getApplication(), finalId)
                _snackbarMessage.value = if (_isMyanmarLanguage.value)
                    "လုပ်ဆောင်ရန် သိမ်းဆည်းပြီးပါပြီ"
                else "Task saved"
            }

            TaskWidgetProvider.refreshAllWidgets(getApplication())
            closeAddModal()
        }
    }

    fun toggleTodoCompleted(todo: TodoEntity) {
        viewModelScope.launch {
            val newCompleted = !todo.isCompleted
            val completedTime = if (newCompleted) System.currentTimeMillis() else null
            todoDao.setCompleted(todo.id, newCompleted, completedTime)

            if (newCompleted) {
                // Cancel scheduled reminder if completed
                ReminderScheduler.cancelReminder(getApplication(), todo.id)
                _snackbarMessage.value = if (_isMyanmarLanguage.value)
                    "✓ ပြီးစီးကြောင်း မှတ်သားပြီးပါပြီ"
                else "✓ Marked as completed"
            }

            TaskWidgetProvider.refreshAllWidgets(getApplication())
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication(), todo.id)
            todoDao.deleteTodo(todo)
            TaskWidgetProvider.refreshAllWidgets(getApplication())
            _snackbarMessage.value = if (_isMyanmarLanguage.value)
                "လုပ်ဆောင်ရန် ဖျက်လိုက်ပါပြီ"
            else "Task deleted"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun speakTask(todo: TodoEntity) {
        val textToSpeak = buildString {
            append(todo.title)
            if (todo.notes.isNotBlank()) {
                append(". ")
                append(todo.notes)
            }
        }
        voiceManager.speakText(textToSpeak)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
