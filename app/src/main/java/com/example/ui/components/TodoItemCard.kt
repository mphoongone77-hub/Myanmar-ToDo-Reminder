package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TodoEntity
import com.example.ui.theme.CategoryGeneralColor
import com.example.ui.theme.CategoryHealthColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CategoryShoppingColor
import com.example.ui.theme.CategoryStudyColor
import com.example.ui.theme.CategoryWorkColor
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoItemCard(
    todo: TodoEntity,
    isMyanmar: Boolean,
    isPlayingVoiceMemo: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPlayVoiceMemo: (String) -> Unit,
    onSpeakTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val categoryColor = when (todo.category) {
        "အလုပ်", "Work" -> CategoryWorkColor
        "ကိုယ်ရေး", "Personal" -> CategoryPersonalColor
        "ဈေးဝယ်", "Shopping" -> CategoryShoppingColor
        "ကျန်းမာရေး", "Health" -> CategoryHealthColor
        "ပညာရေး", "Study" -> CategoryStudyColor
        else -> CategoryGeneralColor
    }

    val priorityColor = when (todo.priority) {
        2 -> PriorityHighColor
        1 -> PriorityMediumColor
        else -> PriorityLowColor
    }

    val priorityLabel = when (todo.priority) {
        2 -> if (isMyanmar) "အရေးကြီး" else "High"
        1 -> if (isMyanmar) "သာမန်" else "Medium"
        else -> if (isMyanmar) "နိမ့်" else "Low"
    }

    val cardAlpha = if (todo.isCompleted) 0.65f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("todo_card_${todo.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (todo.isCompleted) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category chip, Priority chip, and Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = todo.category,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Priority Badge
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = priorityLabel,
                                color = priorityColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Actions: TTS speak & More options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSpeakTask,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("speak_task_btn_${todo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMyanmar) "အသံထွက်ဖတ်ပြပါ" else "Read aloud",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("menu_btn_${todo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isMyanmar) "ပြင်ဆင်မည်" else "Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isMyanmar) "ဖျက်ပစ်မည်" else "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checkbox and Title Row
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("checkbox_${todo.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                            color = if (todo.isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (todo.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todo.notes,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                            ),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Voice Memo Playback button (if attached)
            if (!todo.voiceNotePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { onPlayVoiceMemo(todo.voiceNotePath) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPlayingVoiceMemo) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlayingVoiceMemo) {
                            if (isMyanmar) "အသံမှတ်တမ်း ဖွင့်နေသည်..." else "Playing voice memo..."
                        } else {
                            if (isMyanmar) "မြန်မာလို အသံမှတ်တမ်း နားထောင်မည်" else "Listen to voice memo"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Reminder & Due Date Footer
            if (todo.reminderTimeMillis != null || todo.dueDateMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (todo.reminderTimeMillis != null) {
                        val isOverdue = !todo.isCompleted && todo.reminderTimeMillis < System.currentTimeMillis()
                        val reminderFormatted = formatDateTime(todo.reminderTimeMillis, isMyanmar)
                        Surface(
                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminderFormatted,
                                    fontSize = 11.sp,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (todo.repeatInterval != "NONE") {
                        val repeatLabel = when (todo.repeatInterval) {
                            "DAILY" -> if (isMyanmar) "နေ့စဉ်" else "Daily"
                            "WEEKLY" -> if (isMyanmar) "အပတ်စဉ်" else "Weekly"
                            else -> todo.repeatInterval
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔁 $repeatLabel",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateTime(millis: Long, isMyanmar: Boolean): String {
    val date = Date(millis)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    val nowCal = java.util.Calendar.getInstance()
    val targetCal = java.util.Calendar.getInstance().apply { time = date }

    val isToday = nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
            nowCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)

    val isTomorrow = nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
            nowCal.get(java.util.Calendar.DAY_OF_YEAR) + 1 == targetCal.get(java.util.Calendar.DAY_OF_YEAR)

    return when {
        isToday -> if (isMyanmar) "ဒီကနေ့ ${timeFormat.format(date)}" else "Today ${timeFormat.format(date)}"
        isTomorrow -> if (isMyanmar) "မနက်ဖြန် ${timeFormat.format(date)}" else "Tomorrow ${timeFormat.format(date)}"
        else -> "${dateFormat.format(date)}, ${timeFormat.format(date)}"
    }
}
