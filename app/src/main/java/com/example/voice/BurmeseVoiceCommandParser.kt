package com.example.voice

import java.util.Calendar

data class ParsedVoiceTask(
    val rawText: String,
    val title: String,
    val category: String,
    val dueTime: Long?,
    val priority: Int,
    val commandExplanation: String
)

object BurmeseVoiceCommandParser {

    /**
     * Parses a spoken Burmese text into structured task components:
     * title, category, scheduled time, and priority.
     */
    fun parseBurmeseCommand(spokenText: String): ParsedVoiceTask {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) {
            return ParsedVoiceTask(
                rawText = "",
                title = "လုပ်ဆောင်ရန် အသစ်",
                category = "အထွေထွေ",
                dueTime = null,
                priority = 1,
                commandExplanation = ""
            )
        }

        var cleanedTitle = trimmed

        // Strip common prefix phrases in Burmese
        val prefixesToRemove = listOf(
            "လုပ်ဆောင်ရန် အသစ် ထည့်မည်",
            "လုပ်ဆောင်ရန် အသစ်ထည့်မည်",
            "လုပ်ဆောင်ရန် ထည့်မည်",
            "အလုပ် အသစ် ထည့်မည်",
            "အလုပ် အသစ်ထည့်မည်",
            "အလုပ်သစ် ထည့်မည်",
            "အလုပ် ထည့်ပေးပါ",
            "သတိပေးပါ",
            "မှတ်ထားပါ",
            "အသစ်ထည့်မည်",
            "Add new task",
            "Add task",
            "Remind me to",
            "Remember to"
        )

        for (prefix in prefixesToRemove) {
            if (cleanedTitle.startsWith(prefix, ignoreCase = true)) {
                cleanedTitle = cleanedTitle.substring(prefix.length).trim()
                break
            }
        }

        // Determine Category from keywords
        val category = when {
            cleanedTitle.contains("အလုပ်") || cleanedTitle.contains("ရုံး") ||
            cleanedTitle.contains("meeting") || cleanedTitle.contains("အစည်းအဝေး") ||
            cleanedTitle.contains("စာရင်း") || cleanedTitle.contains("တင်ပြ") -> "အလုပ်"

            cleanedTitle.contains("ဈေး") || cleanedTitle.contains("ဝယ်") ||
            cleanedTitle.contains("shopping") || cleanedTitle.contains("ဝယ်ယူ") -> "ဈေးဝယ်"

            cleanedTitle.contains("ဆေး") || cleanedTitle.contains("ဆရာဝန်") ||
            cleanedTitle.contains("လေ့ကျင့်ခန်း") || cleanedTitle.contains("ရေသောက်") ||
            cleanedTitle.contains("ကျန်းမာရေး") || cleanedTitle.contains("လမ်းလျှောက်") -> "ကျန်းမာရေး"

            cleanedTitle.contains("စာ") || cleanedTitle.contains("စာမေးပွဲ") ||
            cleanedTitle.contains("ကျောင်း") || cleanedTitle.contains("ဖတ်") ||
            cleanedTitle.contains("ပညာရေး") || cleanedTitle.contains("လေ့လာ") -> "ပညာရေး"

            cleanedTitle.contains("မိသားစု") || cleanedTitle.contains("အိမ်") ||
            cleanedTitle.contains("မိတ်ဆွေ") || cleanedTitle.contains("ကိုယ်ရေး") -> "ကိုယ်ရေး"

            else -> "အထွေထွေ"
        }

        // Determine Priority
        val priority = when {
            cleanedTitle.contains("အရေးကြီး") || cleanedTitle.contains("အရေးပေါ်") ||
            cleanedTitle.contains("အမြန်") || cleanedTitle.contains("urgent") ||
            cleanedTitle.contains("important") -> 2 // High

            cleanedTitle.contains("အေးဆေး") || cleanedTitle.contains("မလောပါ") ||
            cleanedTitle.contains("နိမ့်") -> 0 // Low

            else -> 1 // Medium
        }

        // Determine Scheduled Due / Reminder Time
        var scheduledTime: Long? = null
        val explanations = mutableListOf<String>()

        val now = Calendar.getInstance()

        if (cleanedTitle.contains("မနက်ဖြန်") || cleanedTitle.contains("tomorrow", ignoreCase = true)) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                when {
                    cleanedTitle.contains("ညနေ") -> {
                        set(Calendar.HOUR_OF_DAY, 18)
                        set(Calendar.MINUTE, 0)
                        explanations.add("မနက်ဖြန် ညနေ ၆:၀၀")
                    }
                    cleanedTitle.contains("နေ့လယ်") -> {
                        set(Calendar.HOUR_OF_DAY, 13)
                        set(Calendar.MINUTE, 0)
                        explanations.add("မနက်ဖြန် နေ့လယ် ၁:၀၀")
                    }
                    else -> {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        explanations.add("မနက်ဖြန် မနက် ၉:၀၀")
                    }
                }
                set(Calendar.SECOND, 0)
            }
            scheduledTime = cal.timeInMillis
        } else if (cleanedTitle.contains("ဒီညနေ") || cleanedTitle.contains("ဒီည") || cleanedTitle.contains("tonight", ignoreCase = true)) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.HOUR_OF_DAY, 2)
                }
            }
            scheduledTime = cal.timeInMillis
            explanations.add("ဒီညနေ ၆:၀၀")
        } else if (cleanedTitle.contains("၁ နာရီအကြာ") || cleanedTitle.contains("တစ်နာရီအကြာ")) {
            scheduledTime = now.timeInMillis + (60 * 60 * 1000L)
            explanations.add("၁ နာရီအကြာ")
        }

        if (category != "အထွေထွေ") {
            explanations.add("အမျိုးအစား: $category")
        }
        if (priority == 2) {
            explanations.add("အရေးကြီးမှု: မြင့်မား")
        }

        val explanationText = if (explanations.isNotEmpty()) {
            explanations.joinToString(" • ")
        } else {
            "အသံဖြင့် ဖမ်းယူထားသည်"
        }

        // Fallback title if everything was stripped
        val finalTitle = cleanedTitle.ifBlank { trimmed }

        return ParsedVoiceTask(
            rawText = trimmed,
            title = finalTitle,
            category = category,
            dueTime = scheduledTime,
            priority = priority,
            commandExplanation = explanationText
        )
    }
}
