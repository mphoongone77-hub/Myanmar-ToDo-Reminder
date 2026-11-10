package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.data.CategoryEntity
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TodoEntity
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoDialog(
    initialTodo: TodoEntity?,
    categories: List<CategoryEntity> = emptyList(),
    isMyanmar: Boolean,
    isRecordingMemo: Boolean,
    isPlayingMemo: Boolean,
    onStartVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> String?,
    onPlayMemo: (String) -> Unit,
    onDictateTitle: () -> Unit,
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        category: String,
        priority: Int,
        dueDateMillis: Long?,
        reminderTimeMillis: Long?,
        repeatInterval: String,
        voiceNotePath: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTodo?.title ?: "") }
    var notes by remember { mutableStateOf(initialTodo?.notes ?: "") }
    var category by remember { mutableStateOf(initialTodo?.category ?: if (isMyanmar) "အထွေထွေ" else "General") }
    var priority by remember { mutableIntStateOf(initialTodo?.priority ?: 1) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    var hasReminder by remember { mutableStateOf(initialTodo?.reminderTimeMillis != null) }
    var reminderCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (initialTodo?.reminderTimeMillis != null) {
                    timeInMillis = initialTodo.reminderTimeMillis
                } else {
                    add(Calendar.HOUR_OF_DAY, 1)
                }
            }
        )
    }

    var repeatInterval by remember { mutableStateOf(initialTodo?.repeatInterval ?: "NONE") }
    var voiceNotePath by remember { mutableStateOf(initialTodo?.voiceNotePath) }

    val categoryNames = if (categories.isNotEmpty()) {
        categories.map { if (isMyanmar) it.nameMm else it.name }
    } else {
        if (isMyanmar) {
            listOf("အထွေထွေ", "အလုပ်", "ကိုယ်ရေး", "ဈေးဝယ်", "ကျန်းမာရေး", "ပညာရေး")
        } else {
            listOf("General", "Work", "Personal", "Shopping", "Health", "Study")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("add_edit_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTodo == null) {
                            if (isMyanmar) "လုပ်ဆောင်ရန် အသစ်ထည့်မည်" else "Add New Task"
                        } else {
                            if (isMyanmar) "လုပ်ဆောင်ရန် ပြင်ဆင်မည်" else "Edit Task"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input with Voice Dictate button
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isMyanmar) "ခေါင်းစဉ် (Title) *" else "Title *") },
                    placeholder = { Text(if (isMyanmar) "ဥပမာ- ဆေးသောက်ရန်၊ စာအုပ်ဖတ်ရန်" else "e.g., Read book, buy groceries") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("todo_title_input"),
                    trailingIcon = {
                        IconButton(
                            onClick = onDictateTitle,
                            modifier = Modifier.testTag("dictate_title_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isMyanmar) "မြန်မာလို အသံဖြင့်ပြောမည်" else "Speak",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isMyanmar) "အသေးစိတ် မှတ်ချက် (Notes)" else "Notes") },
                    placeholder = { Text(if (isMyanmar) "ထပ်ဆောင်းမှတ်ချက်များ ထည့်သွင်းနိုင်သည်" else "Add optional notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("todo_notes_input"),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection Dropdown
                Text(
                    text = if (isMyanmar) "အမျိုးအစား ရွေးချယ်မှု (Category)" else "Category Selection",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_dropdown_box")
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isMyanmar) "အမျိုးအစား" else "Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("category_dropdown_input")
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        modifier = Modifier.testTag("category_dropdown_menu")
                    ) {
                        categoryNames.forEach { catName ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = if (category == catName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catName,
                                            fontWeight = if (category == catName) FontWeight.Bold else FontWeight.Normal,
                                            color = if (category == catName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    category = catName
                                    isCategoryDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_item_$catName")
                            )
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isMyanmar) "+ အမျိုးအစားအသစ် ထည့်မည်..." else "+ Create New Category...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isCategoryDropdownExpanded = false
                                showNewCategoryInput = true
                            },
                            modifier = Modifier.testTag("dropdown_create_category_item")
                        )
                    }
                }

                // Inline Custom Category Input when requested
                if (showNewCategoryInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            placeholder = { Text(if (isMyanmar) "အမျိုးအစား အမည်" else "New category name") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inline_new_category_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val trimmed = customCategoryText.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddCustomCategory(trimmed)
                                    category = trimmed
                                    customCategoryText = ""
                                    showNewCategoryInput = false
                                }
                            },
                            enabled = customCategoryText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("inline_save_category_btn")
                        ) {
                            Text(if (isMyanmar) "ထည့်မည်" else "Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryNames.take(8).forEach { catName ->
                        FilterChip(
                            selected = category == catName,
                            onClick = { category = catName },
                            label = { Text(catName, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_cat_$catName")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = if (isMyanmar) "အရေးကြီးမှု (Priority)" else "Priority",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val priorities = listOf(
                        Triple(0, if (isMyanmar) "နိမ့်" else "Low", PriorityLowColor),
                        Triple(1, if (isMyanmar) "သာမန်" else "Medium", PriorityMediumColor),
                        Triple(2, if (isMyanmar) "အရေးကြီး" else "High", PriorityHighColor)
                    )

                    priorities.forEach { (level, label, color) ->
                        FilterChip(
                            selected = priority == level,
                            onClick = { priority = level },
                            label = { Text(label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMyanmar) "အချိန်သတိပေးချက် (Reminder)" else "Reminder Alarm",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { hasReminder = it },
                                modifier = Modifier.testTag("reminder_switch")
                            )
                        }

                        if (hasReminder) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick preset chips
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        reminderCalendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                                    },
                                    label = { Text(if (isMyanmar) "၁ နာရီအကြာ" else "+1 Hour") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        reminderCalendar = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 18)
                                            set(Calendar.MINUTE, 0)
                                            if (timeInMillis <= System.currentTimeMillis()) {
                                                add(Calendar.DAY_OF_YEAR, 1)
                                            }
                                        }
                                    },
                                    label = { Text(if (isMyanmar) "ညနေ ၆:၀၀" else "Today 6 PM") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        reminderCalendar = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, 9)
                                            set(Calendar.MINUTE, 0)
                                        }
                                    },
                                    label = { Text(if (isMyanmar) "မနက်ဖြန် မနက် ၉:၀၀" else "Tomorrow 9 AM") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Date & Time pickers
                            val dateFormat = SimpleDateFormat("yyyy MMM dd", Locale.getDefault())
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                reminderCalendar = (reminderCalendar.clone() as Calendar).apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                }
                                            },
                                            reminderCalendar.get(Calendar.YEAR),
                                            reminderCalendar.get(Calendar.MONTH),
                                            reminderCalendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dateFormat.format(reminderCalendar.time), fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                reminderCalendar = (reminderCalendar.clone() as Calendar).apply {
                                                    set(Calendar.HOUR_OF_DAY, hour)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                }
                                            },
                                            reminderCalendar.get(Calendar.HOUR_OF_DAY),
                                            reminderCalendar.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(timeFormat.format(reminderCalendar.time), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Repeat chips
                            Text(
                                text = if (isMyanmar) "ထပ်ခါသတိပေးမှု (Repeat)" else "Repeat",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val repeats = listOf(
                                    "NONE" to (if (isMyanmar) "တစ်ကြိမ်သာ" else "Once"),
                                    "DAILY" to (if (isMyanmar) "နေ့စဉ်" else "Daily"),
                                    "WEEKLY" to (if (isMyanmar) "အပတ်စဉ်" else "Weekly")
                                )
                                repeats.forEach { (key, label) ->
                                    FilterChip(
                                        selected = repeatInterval == key,
                                        onClick = { repeatInterval = key },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Burmese Voice Memo Attachment Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isMyanmar) "🎙️ မြန်မာလို အသံမှတ်တမ်း (Voice Memo)" else "🎙️ Voice Memo",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMyanmar)
                                "မိမိစိတ်ကြိုက် မြန်မာလို အသံမှတ်တမ်း သွင်းထားနိုင်ပါသည်"
                            else "Record your voice reminder directly",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (voiceNotePath.isNullOrBlank()) {
                            // Record Button
                            Button(
                                onClick = {
                                    if (isRecordingMemo) {
                                        val path = onStopVoiceRecord()
                                        if (!path.isNullOrBlank()) {
                                            voiceNotePath = path
                                        }
                                    } else {
                                        onStartVoiceRecord()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecordingMemo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("record_memo_btn")
                            ) {
                                Icon(
                                    imageVector = if (isRecordingMemo) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isRecordingMemo) {
                                        if (isMyanmar) "အသံဖမ်းနေသည်... (ရပ်တန့်ရန် နှိပ်ပါ)" else "Recording... (Tap to stop)"
                                    } else {
                                        if (isMyanmar) "မြန်မာလို အသံသွင်းမည် (Tap to Record)" else "Start Voice Recording"
                                    }
                                )
                            }
                        } else {
                            // Audio Recorded Actions: Play, Re-record, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onPlayMemo(voiceNotePath!!) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingMemo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isPlayingMemo) {
                                            if (isMyanmar) "ဖွင့်နေသည်..." else "Playing..."
                                        } else {
                                            if (isMyanmar) "နားထောင်မည်" else "Play"
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        voiceNotePath = null
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete audio", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Actions: Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_btn")
                    ) {
                        Text(if (isMyanmar) "မလုပ်တော့ပါ" else "Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    notes,
                                    category,
                                    priority,
                                    if (hasReminder) reminderCalendar.timeInMillis else null,
                                    if (hasReminder) reminderCalendar.timeInMillis else null,
                                    repeatInterval,
                                    voiceNotePath
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_todo_btn")
                    ) {
                        Text(if (isMyanmar) "သိမ်းဆည်းမည်" else "Save")
                    }
                }
            }
        }
    }
}
