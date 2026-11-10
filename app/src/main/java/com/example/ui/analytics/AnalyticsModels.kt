package com.example.ui.analytics

import androidx.compose.ui.graphics.Color
import com.example.data.TodoEntity
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayCompletionData(
    val dayLabel: String,
    val dateFormatted: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionRate: Float,
    val isToday: Boolean
)

data class PriorityDistributionData(
    val priority: Int,
    val labelMm: String,
    val labelEn: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)

data class WeeklyAnalyticsReport(
    val overallWeeklyRate: Float,
    val completedThisWeek: Int,
    val totalThisWeek: Int,
    val streakDays: Int,
    val dailyBreakdown: List<DayCompletionData>,
    val priorityDistribution: List<PriorityDistributionData>,
    val categoryCounts: Map<String, Int>
)

object AnalyticsCalculator {

    fun computeWeeklyAnalytics(todos: List<TodoEntity>, isMyanmar: Boolean): WeeklyAnalyticsReport {
        val nowCal = Calendar.getInstance()
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        // Generate past 7 days (including today)
        val dailyList = mutableListOf<DayCompletionData>()
        val dayNameFormatMm = listOf("တနင်္ဂနွေ", "တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ")
        val dayNameFormatEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        var totalWeeklyCount = 0
        var completedWeeklyCount = 0

        // 7 days window (from 6 days ago up to today)
        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = targetCal.timeInMillis
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = targetCal.timeInMillis

            val isToday = (todayYear == targetCal.get(Calendar.YEAR) &&
                    todayDayOfYear == (targetCal.get(Calendar.DAY_OF_YEAR) - 1))

            val dayOfWeekIndex = (targetCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // adjust for 0-indexed Sun-Sat
            val dayName = if (isMyanmar) dayNameFormatMm[dayOfWeekIndex] else dayNameFormatEn[dayOfWeekIndex]

            val dateFormat = SimpleDateFormat("d/M", Locale.getDefault())
            val dateFormatted = dateFormat.format(Date(startOfDay))

            // Tasks due or created on this day
            val tasksOnDay = todos.filter { todo ->
                val due = todo.dueDateMillis ?: todo.createdAtMillis
                due in startOfDay until endOfDay
            }

            val completedOnDay = tasksOnDay.count { it.isCompleted }
            val countOnDay = tasksOnDay.size

            totalWeeklyCount += countOnDay
            completedWeeklyCount += completedOnDay

            val rate = if (countOnDay > 0) completedOnDay.toFloat() / countOnDay.toFloat() else 0f

            dailyList.add(
                DayCompletionData(
                    dayLabel = dayName,
                    dateFormatted = dateFormatted,
                    completedCount = completedOnDay,
                    totalCount = countOnDay,
                    completionRate = rate,
                    isToday = isToday
                )
            )
        }

        val weeklyRate = if (totalWeeklyCount > 0) completedWeeklyCount.toFloat() / totalWeeklyCount.toFloat() else 0f

        // Priority Distribution
        val totalTodos = todos.size
        val highCount = todos.count { it.priority == 2 }
        val medCount = todos.count { it.priority == 1 }
        val lowCount = todos.count { it.priority == 0 }

        val priorityDistribution = listOf(
            PriorityDistributionData(
                priority = 2,
                labelMm = "အရေးကြီး (High)",
                labelEn = "High Priority",
                count = highCount,
                percentage = if (totalTodos > 0) highCount.toFloat() / totalTodos else 0f,
                color = PriorityHighColor
            ),
            PriorityDistributionData(
                priority = 1,
                labelMm = "သာမန် (Medium)",
                labelEn = "Medium Priority",
                count = medCount,
                percentage = if (totalTodos > 0) medCount.toFloat() / totalTodos else 0f,
                color = PriorityMediumColor
            ),
            PriorityDistributionData(
                priority = 0,
                labelMm = "နိမ့် (Low)",
                labelEn = "Low Priority",
                count = lowCount,
                percentage = if (totalTodos > 0) lowCount.toFloat() / totalTodos else 0f,
                color = PriorityLowColor
            )
        )

        // Category breakdown
        val categoryCounts = todos.groupBy { it.category }.mapValues { it.value.size }

        // Streak calculation (days with at least 1 completed task)
        var streak = 0
        for (day in dailyList.reversed()) {
            if (day.completedCount > 0) streak++ else break
        }

        return WeeklyAnalyticsReport(
            overallWeeklyRate = weeklyRate,
            completedThisWeek = completedWeeklyCount,
            totalThisWeek = totalWeeklyCount,
            streakDays = streak,
            dailyBreakdown = dailyList,
            priorityDistribution = priorityDistribution,
            categoryCounts = categoryCounts
        )
    }
}
