package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "todo_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val todoId = intent.getLongExtra(ReminderScheduler.EXTRA_TODO_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_TITLE) ?: "သတိပေးချက် (Reminder)"
        val notes = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_NOTES) ?: ""
        val category = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_CATEGORY) ?: "အထွေထွေ"
        val voicePath = intent.getStringExtra(ReminderScheduler.EXTRA_VOICE_NOTE_PATH)
        val repeatInterval = intent.getStringExtra(ReminderScheduler.EXTRA_REPEAT_INTERVAL) ?: "NONE"

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_MARK_DONE -> {
                        val notifId = if (todoId > 0) (todoId % 100000).toInt() else 1001
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notifId)
                        ReminderScheduler.cancelReminder(context, todoId)
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getDatabase(context)
                            db.todoDao().setCompleted(todoId, true, System.currentTimeMillis())
                        }
                    }
                    ReminderScheduler.ACTION_SNOOZE -> {
                        val notifId = if (todoId > 0) (todoId % 100000).toInt() else 1001
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notifId)
                        ReminderScheduler.snoozeReminder(context, todoId, title, notes, category, voicePath, repeatInterval, 10)
                    }
                    ReminderScheduler.ACTION_SPEAK_REMINDER -> {
                        BurmeseReminderTtsSpeaker.speakReminder(context, title, notes, category)
                    }
                    ReminderScheduler.ACTION_TODO_REMINDER -> {
                        // Display the notification card with action buttons
                        showNotification(context, todoId, title, notes, category, voicePath)

                        // Vocalize the task reminder in Burmese using TextToSpeech
                        BurmeseReminderTtsSpeaker.speakReminder(context, title, notes, category)

                        // Reschedule next occurrence for recurring reminders (Daily / Weekly)
                        if (repeatInterval.equals("DAILY", ignoreCase = true) || repeatInterval.equals("WEEKLY", ignoreCase = true)) {
                            val nextRepeatTime = ReminderScheduler.calculateNextRepeatTime(System.currentTimeMillis(), repeatInterval)
                            if (nextRepeatTime != null) {
                                ReminderScheduler.scheduleAtSpecificTime(
                                    context = context,
                                    taskId = todoId,
                                    title = title,
                                    notes = notes,
                                    category = category,
                                    voicePath = voicePath,
                                    repeatInterval = repeatInterval,
                                    triggerAtMillis = nextRepeatTime
                                )
                                CoroutineScope(Dispatchers.IO).launch {
                                    val db = AppDatabase.getDatabase(context)
                                    val existing = db.todoDao().getTodoById(todoId)
                                    if (existing != null) {
                                        db.todoDao().updateTodo(
                                            existing.copy(
                                                dueDateMillis = nextRepeatTime,
                                                reminderTimeMillis = nextRepeatTime
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        todoId: Long,
        title: String,
        notes: String,
        category: String,
        voicePath: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "လုပ်ဆောင်ရန် သတိပေးချက်များ (Todo Reminders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "အလုပ်များနှင့် အစီအစဉ်များ သတိပေးသည့် စနစ်"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notifId = if (todoId > 0) (todoId % 100000).toInt() else 1001

        // Content intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TODO_ID", todoId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done Intent
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_MARK_DONE
            putExtra(ReminderScheduler.EXTRA_TODO_ID, todoId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId + 10000),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10m Intent
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE
            putExtra(ReminderScheduler.EXTRA_TODO_ID, todoId)
            putExtra(ReminderScheduler.EXTRA_TODO_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_TODO_NOTES, notes)
            putExtra(ReminderScheduler.EXTRA_TODO_CATEGORY, category)
            putExtra(ReminderScheduler.EXTRA_VOICE_NOTE_PATH, voicePath)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId + 20000),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Speak aloud again Intent
        val speakIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SPEAK_REMINDER
            putExtra(ReminderScheduler.EXTRA_TODO_ID, todoId)
            putExtra(ReminderScheduler.EXTRA_TODO_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_TODO_NOTES, notes)
            putExtra(ReminderScheduler.EXTRA_TODO_CATEGORY, category)
        }
        val speakPendingIntent = PendingIntent.getBroadcast(
            context,
            (notifId + 30000),
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayContent = if (notes.isNotBlank()) {
            "[$category] $notes"
        } else {
            "[$category] လုပ်ဆောင်ရန် အချိန်ရောက်ရှိပါပြီ"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell) // Clean monochrome vector icon
            .setContentTitle("🔔 $title")
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(contentPendingIntent)
            .addAction(0, "✓ ပြီးစီးပြီ (Done)", donePendingIntent)
            .addAction(0, "⏱ ၁၀ မိနစ် ရွှေ့မည် (Snooze)", snoozePendingIntent)
            .addAction(0, "🔊 အသံဖြင့်ဖတ်ပြပါ (Speak)", speakPendingIntent)
            .build()

        notificationManager.notify(notifId, notification)
    }
}
