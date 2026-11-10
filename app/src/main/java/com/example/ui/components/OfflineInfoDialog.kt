package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineInfoDialog(
    isOnline: Boolean,
    isMyanmar: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("offline_info_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isMyanmar) "၁၀၀% အော့ဖ်လိုင်း အသုံးပြုနိုင်မှု" else "100% Offline Capability",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (!isOnline) {
                                if (isMyanmar) "လက်ရှိ အော့ဖ်လိုင်းဖြင့် သုံးစွဲနေပါသည်" else "Currently using in Offline Mode"
                            } else {
                                if (isMyanmar) "အင်တာနက် မရှိလည်း အပြည့်အဝ သုံးနိုင်ပါသည်" else "Fully works without Internet"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (!isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Explanation Features
            OfflineFeatureItem(
                icon = Icons.Default.Storage,
                title = if (isMyanmar) "စက်တွင်း ဒေတာဘေ့စ် (Local Storage)" else "On-Device Storage (Room)",
                description = if (isMyanmar)
                    "အလုပ်ခေါင်းစဉ်များ၊ မှတ်စုများနှင့် အချက်အလက်အားလုံးကို သင့်ဖုန်းတွင်း၌သာ လုံခြုံစွာ သိမ်းဆည်းထားသဖြင့် အင်တာနက်မရှိဘဲ အချိန်မရွေး ကြည့်ရှု/ပြင်ဆင်နိုင်ပါသည်။"
                else
                    "All tasks and notes are saved locally in the device's Room database. Zero cloud latency and completely functional without connection."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OfflineFeatureItem(
                icon = Icons.Default.Alarm,
                title = if (isMyanmar) "အော့ဖ်လိုင်း သတိပေးနှိုးစက် (AlarmManager)" else "Offline Alarms & Reminders",
                description = if (isMyanmar)
                    "Android စနစ်၏ Hardware AlarmManager ဖြင့် အလုပ်လုပ်သောကြောင့် အင်တာနက်ပိတ်ထားချိန် သို့မဟုတ် လေယာဉ်ပျံစနစ် (Airplane Mode) ဖွင့်ထားချိန်တွင်ပင် သတ်မှတ်ချိန်၌ တိကျစွာ အချက်ပေးမြည်ပါမည်။"
                else
                    "Exact alarms are scheduled with Android's AlarmManager directly on device hardware, triggering even in Airplane Mode or without Wi-Fi."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OfflineFeatureItem(
                icon = Icons.Default.Mic,
                title = if (isMyanmar) "အော့ဖ်လိုင်း အသံဖိုင်မှတ်စု (Voice Memos)" else "Offline Voice Memos",
                description = if (isMyanmar)
                    "အင်တာနက်မရှိချိန်တွင် အလုပ်အသစ်ထည့်သွင်းရာ၌ 'အသံမှတ်စု' ခလုတ်ဖြင့် မိမိအသံကို တိုက်ရိုက် အသံသွင်းမှတ်တမ်းတင်နိုင်ပြီး အော့ဖ်လိုင်း ပြန်လည်နားထောင်နိုင်ပါသည်။"
                else
                    "Record voice memos directly to your phone's local storage and play them back anytime without requiring any network."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OfflineFeatureItem(
                icon = Icons.Default.BarChart,
                title = if (isMyanmar) "စက်တွင်း ဇယားများနှင့် သုံးသပ်ချက် (Local Analytics)" else "Local Visual Analytics",
                description = if (isMyanmar)
                    "၇ ရက်တာ ပြီးစီးမှုနှုန်း၊ အရေးကြီးမှုအဆင့် ခွဲခြမ်းစိတ်ဖြာမှုနှင့် ကဏ္ဍအလိုက် ဇယားများကို ဖုန်းထဲရှိ စာရင်းများဖြင့် အော့ဖ်လိုင်း တိုက်ရိုက် တွက်ချက်ပြသပေးပါသည်။"
                else
                    "Weekly completion rates and priority charts are computed instantly on-device without contacting any external servers."
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dismiss_offline_info_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMyanmar) "နားလည်ပါပြီ" else "Got It",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun OfflineFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
