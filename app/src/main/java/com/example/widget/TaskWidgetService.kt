package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TodoEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskRemoteViewsFactory(applicationContext, intent)
    }
}

class TaskRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<TodoEntity> = emptyList()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        // Query database on background binder thread
        try {
            val db = AppDatabase.getDatabase(context)
            tasks = db.todoDao().getPendingTodosSync()
        } catch (e: Exception) {
            tasks = emptyList()
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= tasks.size) return null
        val task = tasks[position]

        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        // Set title
        views.setTextViewText(R.id.widget_item_title, task.title)

        // Set category & due time subtitle
        val subtitle = formatTaskSubtitle(task)
        views.setTextViewText(R.id.widget_item_subtitle, subtitle)

        // Priority bar indicator color
        val priorityColor = when (task.priority) {
            2 -> Color.parseColor("#E53935") // High: Red
            1 -> Color.parseColor("#FB8C00") // Medium: Orange
            else -> Color.parseColor("#43A047") // Low: Green
        }
        views.setInt(R.id.widget_item_priority_bar, "setBackgroundColor", priorityColor)

        // Fill-in intent for Mark Complete Button
        val completeFillIn = Intent().apply {
            action = TaskWidgetProvider.ACTION_MARK_COMPLETE
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_complete_btn, completeFillIn)

        // Fill-in intent for clicking the task row to open the app
        val itemClickFillIn = Intent().apply {
            action = TaskWidgetProvider.ACTION_OPEN_APP
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, itemClickFillIn)

        return views
    }

    private fun formatTaskSubtitle(task: TodoEntity): String {
        val targetTime = task.dueDateMillis ?: task.reminderTimeMillis
        val timeStr = if (targetTime != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = targetTime }
            val nowCal = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
            ) {
                "⏰ ယနေ့ ${timeFormat.format(Date(targetTime))}"
            } else {
                val dateFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
                "⏰ ${dateFormat.format(Date(targetTime))}"
            }
        } else {
            "⏰ သတ်မှတ်ချိန်မရှိ"
        }

        val repeatStr = when (task.repeatInterval.uppercase()) {
            "DAILY" -> " | 🔁 နေ့စဉ်"
            "WEEKLY" -> " | 🔁 အပတ်စဉ်"
            else -> ""
        }

        return "$timeStr | 🏷️ ${task.category}$repeatStr"
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in tasks.indices) tasks[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
