package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TodoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    const val ACTION_TODO_REMINDER = "com.example.ACTION_TODO_REMINDER"
    const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
    const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
    const val ACTION_SPEAK_REMINDER = "com.example.ACTION_SPEAK_REMINDER"

    const val EXTRA_TODO_ID = "extra_todo_id"
    const val EXTRA_TODO_TITLE = "extra_todo_title"
    const val EXTRA_TODO_NOTES = "extra_todo_notes"
    const val EXTRA_TODO_CATEGORY = "extra_todo_category"
    const val EXTRA_VOICE_NOTE_PATH = "extra_voice_note_path"
    const val EXTRA_REPEAT_INTERVAL = "extra_repeat_interval"

    /**
     * Schedules an alarm with Android's AlarmManager for a Task at its specific due time.
     */
    fun scheduleTask(context: Context, task: Task) {
        val targetDueTime = task.dueTime ?: return
        scheduleAtSpecificTime(
            context = context,
            taskId = task.id,
            title = task.title,
            notes = task.notes,
            category = task.category,
            voicePath = null,
            repeatInterval = task.repeatInterval,
            triggerAtMillis = targetDueTime
        )
    }

    /**
     * Schedules an alarm with Android's AlarmManager for a TodoEntity at its specific due time / reminder time.
     */
    fun scheduleReminder(context: Context, todo: TodoEntity) {
        val targetDueTime = todo.dueDateMillis ?: todo.reminderTimeMillis ?: return
        scheduleAtSpecificTime(
            context = context,
            taskId = todo.id,
            title = todo.title,
            notes = todo.notes,
            category = todo.category,
            voicePath = todo.voiceNotePath,
            repeatInterval = todo.repeatInterval,
            triggerAtMillis = targetDueTime
        )
    }

    /**
     * Calculates the next trigger timestamp for a recurring reminder.
     */
    fun calculateNextRepeatTime(fromTimeMillis: Long, repeatInterval: String): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = fromTimeMillis }
        val now = System.currentTimeMillis()

        return when (repeatInterval.uppercase()) {
            "DAILY" -> {
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            "WEEKLY" -> {
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            else -> null
        }
    }

    /**
     * Internal helper to set exact alarms with AlarmManager and pass recurring repeat parameters.
     */
    fun scheduleAtSpecificTime(
        context: Context,
        taskId: Long,
        title: String,
        notes: String,
        category: String,
        voicePath: String?,
        repeatInterval: String = "NONE",
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d("ReminderScheduler", "Due time $triggerAtMillis is in the past, skipping alarm.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TODO_REMINDER
            putExtra(EXTRA_TODO_ID, taskId)
            putExtra(EXTRA_TODO_TITLE, title)
            putExtra(EXTRA_TODO_NOTES, notes)
            putExtra(EXTRA_TODO_CATEGORY, category)
            putExtra(EXTRA_VOICE_NOTE_PATH, voicePath)
            putExtra(EXTRA_REPEAT_INTERVAL, repeatInterval)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled AlarmManager for task '$title' (ID: $taskId, Repeat: $repeatInterval) at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException scheduling exact alarm, falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Snoozes an alarm for a specified number of minutes.
     */
    fun snoozeReminder(
        context: Context,
        todoId: Long,
        title: String,
        notes: String,
        category: String,
        voicePath: String?,
        repeatInterval: String = "NONE",
        minutes: Int = 10
    ) {
        val triggerAt = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleAtSpecificTime(
            context = context,
            taskId = todoId,
            title = title,
            notes = notes,
            category = category,
            voicePath = voicePath,
            repeatInterval = repeatInterval,
            triggerAtMillis = triggerAt
        )
    }

    /**
     * Cancels an existing alarm by task ID.
     */
    fun cancelReminder(context: Context, todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TODO_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled alarm for task ID: $todoId")
        }
    }

    /**
     * Reschedules all pending tasks from the database that have a future due time.
     */
    fun rescheduleAllPendingTasks(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val now = System.currentTimeMillis()
            val upcomingTasks = db.todoDao().getUpcomingReminders(now)
            for (task in upcomingTasks) {
                scheduleReminder(context, task)
            }
            Log.d("ReminderScheduler", "Rescheduled ${upcomingTasks.size} pending tasks after boot/restart.")
        }
    }
}
