package com.example.data

enum class TaskRepeatInterval(val key: String, val labelMm: String, val labelEn: String) {
    NONE("NONE", "မရှိပါ", "None"),
    DAILY("DAILY", "နေ့စဉ်", "Daily"),
    WEEKLY("WEEKLY", "အပတ်စဉ်", "Weekly");

    companion object {
        fun fromKey(key: String): TaskRepeatInterval {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: NONE
        }
    }
}

/**
 * Task data class representing a user task with ID, title, category, due time, and recurring interval.
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val category: String = "General",
    val dueTime: Long? = null,
    val repeatInterval: String = TaskRepeatInterval.NONE.key,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 1
)

fun Task.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    notes = notes,
    category = category,
    priority = priority,
    dueDateMillis = dueTime,
    reminderTimeMillis = dueTime,
    repeatInterval = repeatInterval,
    isCompleted = isCompleted
)

fun TodoEntity.toTask(): Task = Task(
    id = id,
    title = title,
    category = category,
    dueTime = dueDateMillis ?: reminderTimeMillis,
    repeatInterval = repeatInterval,
    notes = notes,
    isCompleted = isCompleted,
    priority = priority
)
