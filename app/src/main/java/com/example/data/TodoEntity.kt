package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: String = "အထွေထွေ",
    val priority: Int = 1, // 0 = Low, 1 = Medium, 2 = High
    val dueDateMillis: Long? = null,
    val reminderTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val repeatInterval: String = "NONE", // NONE, DAILY, WEEKLY
    val voiceNotePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
