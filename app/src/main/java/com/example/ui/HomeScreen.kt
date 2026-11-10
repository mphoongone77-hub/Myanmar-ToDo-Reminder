package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.TodoEntity
import com.example.ui.analytics.AnalyticsDashboardView
import com.example.ui.components.AddEditTodoDialog
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.components.OfflineInfoDialog
import com.example.ui.components.TaskSearchBar
import com.example.ui.components.TodoItemCard
import com.example.ui.components.VoiceInputDialog
import kotlinx.coroutines.launch

enum class MainScreenTab {
    TASKS,
    ANALYTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(MainScreenTab.TASKS) }

    val todos by viewModel.filteredTodos.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isCategoryManagerOpen by viewModel.isCategoryManagerOpen.collectAsState()
    val isMyanmar by viewModel.isMyanmarLanguage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isOfflineInfoSheetOpen by viewModel.isOfflineInfoSheetOpen.collectAsState()

    val isAddEditModalOpen by viewModel.isAddEditModalOpen.collectAsState()
    val activeEditingTodo by viewModel.activeEditingTodo.collectAsState()
    val isVoiceInputSheetOpen by viewModel.isVoiceInputSheetOpen.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isRecordingMemo by viewModel.voiceManager.isRecording.collectAsState()
    val currentlyPlayingMemoPath by viewModel.voiceManager.currentlyPlayingPath.collectAsState()

    // Handle Back Press from Analytics to Tasks screen
    BackHandler(enabled = currentScreen != MainScreenTab.TASKS) {
        currentScreen = MainScreenTab.TASKS
    }

    // Permission launchers
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openVoiceInputSheet()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (currentScreen == MainScreenTab.ANALYTICS)
                                        Icons.Default.BarChart
                                    else
                                        Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentScreen == MainScreenTab.ANALYTICS) {
                                    if (isMyanmar) "တိုးတက်မှု & ဇယားများ" else "Analytics & Charts"
                                } else {
                                    if (isMyanmar) "လုပ်ဆောင်ရန် & သတိပေးချက်" else "Todo & Reminder"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (currentScreen == MainScreenTab.ANALYTICS) {
                                    if (isMyanmar) "၇ ရက်တာ ပြီးစီးမှု & အဆင့် ခွဲခြမ်းစိတ်ဖြာမှု" else "Weekly completion & priority distribution"
                                } else {
                                    if (isMyanmar) "မြန်မာလို အသံဖြင့် အသုံးပြုနိုင်သည်" else "Burmese Voice Enabled"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                },
                actions = {
                    // Offline / Online Status Indicator Chip
                    Surface(
                        onClick = { viewModel.openOfflineInfo() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (!isOnline) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("offline_status_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = "Connection Status",
                                modifier = Modifier.size(15.dp),
                                tint = if (!isOnline) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!isOnline) {
                                    if (isMyanmar) "အော့ဖ်လိုင်း" else "Offline"
                                } else {
                                    if (isMyanmar) "အွန်လိုင်း" else "Online"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isOnline) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Language switch button (Myanmar / English)
                    Surface(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("toggle_language_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMyanmar) "မြန်မာ" else "English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == MainScreenTab.TASKS,
                    onClick = { currentScreen = MainScreenTab.TASKS },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = if (isMyanmar) "အလုပ်များ" else "Tasks"
                        )
                    },
                    label = {
                        Text(
                            text = if (isMyanmar) "အလုပ်များ" else "Tasks",
                            fontWeight = if (currentScreen == MainScreenTab.TASKS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_item_tasks")
                )

                NavigationBarItem(
                    selected = currentScreen == MainScreenTab.ANALYTICS,
                    onClick = { currentScreen = MainScreenTab.ANALYTICS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = if (isMyanmar) "သုံးသပ်ချက်" else "Analytics"
                        )
                    },
                    label = {
                        Text(
                            text = if (isMyanmar) "သုံးသပ်ချက်" else "Analytics",
                            fontWeight = if (currentScreen == MainScreenTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_item_analytics")
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Quick Voice Button (Burmese voice recognition)
                FloatingActionButton(
                    onClick = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.testTag("quick_voice_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isMyanmar) "အသံဖြင့်ထည့်မည်" else "Add with Voice",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Primary Add Task Button
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddModal() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = if (isMyanmar) "အသစ်ထည့်မည်" else "New Task",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("add_task_fab")
                )
            }
        }
    ) { innerPadding ->
        if (currentScreen == MainScreenTab.ANALYTICS) {
            AnalyticsDashboardView(
                todos = allTodos,
                isMyanmar = isMyanmar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Offline Notice Banner (shows when device has no internet)
                AnimatedVisibility(visible = !isOnline) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            .clickable { viewModel.openOfflineInfo() }
                            .testTag("offline_notice_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMyanmar)
                                    "အော့ဖ်လိုင်း စနစ်: ဒေတာများနှင့် နှိုးစက်များ ၁၀၀% အလုပ်လုပ်ပါသည်"
                                else
                                    "Offline Mode: All tasks & reminders work 100% locally",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Stats & Progress Overview Banner
            StatsBannerCard(
                stats = stats,
                isMyanmar = isMyanmar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search Bar at the top of the task list
            TaskSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                matchCount = todos.size,
                isMyanmar = isMyanmar,
                onVoiceSearch = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    viewModel.openVoiceInputSheet()
                }
            )

            // Category Chips Row
            CategoryScrollRow(
                selectedCategory = selectedCategory,
                categories = categories,
                isMyanmar = isMyanmar,
                onCategorySelected = { viewModel.setSelectedCategory(it) },
                onManageCategories = { viewModel.openCategoryManager() },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Filter Tabs (All, Today, Upcoming, Completed)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                FilterTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setFilterTab(tab) },
                        text = {
                            Text(
                                text = if (isMyanmar) tab.titleMm else tab.titleEn,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tasks List or Empty State
            if (todos.isEmpty()) {
                EmptyStateView(
                    selectedTab = selectedTab,
                    isMyanmar = isMyanmar,
                    onAddTask = { viewModel.openAddModal() },
                    onVoiceAdd = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("todo_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = todos, key = { it.id }) { todo ->
                        TodoItemCard(
                            todo = todo,
                            isMyanmar = isMyanmar,
                            isPlayingVoiceMemo = currentlyPlayingMemoPath == todo.voiceNotePath,
                            onToggleComplete = { viewModel.toggleTodoCompleted(todo) },
                            onEdit = { viewModel.openAddModal(todo) },
                            onDelete = { viewModel.deleteTodo(todo) },
                            onPlayVoiceMemo = { path ->
                                viewModel.voiceManager.playVoiceMemo(path)
                            },
                            onSpeakTask = { viewModel.speakTask(todo) }
                        )
                    }
                }
            }
        }
    }
}

    // Add or Edit Dialog
    if (isAddEditModalOpen) {
        AddEditTodoDialog(
            initialTodo = activeEditingTodo,
            categories = categories,
            isMyanmar = isMyanmar,
            isRecordingMemo = isRecordingMemo,
            isPlayingMemo = currentlyPlayingMemoPath != null,
            onStartVoiceRecord = {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                viewModel.voiceManager.startRecordingVoiceMemo()
            },
            onStopVoiceRecord = {
                viewModel.voiceManager.stopRecordingVoiceMemo()
            },
            onPlayMemo = { path ->
                viewModel.voiceManager.playVoiceMemo(path)
            },
            onDictateTitle = {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                viewModel.openVoiceInputSheet()
            },
            onAddCustomCategory = { newCategory ->
                viewModel.addCustomCategory(newCategory)
            },
            onDismiss = { viewModel.closeAddModal() },
            onSave = { title, notes, category, priority, dueDate, reminderDate, repeat, voicePath ->
                viewModel.saveTodo(
                    id = activeEditingTodo?.id ?: 0L,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    dueDateMillis = dueDate,
                    reminderTimeMillis = reminderDate,
                    repeatInterval = repeat,
                    voiceNotePath = voicePath
                )
            }
        )
    }

    // Voice Input Bottom Sheet
    if (isVoiceInputSheetOpen) {
        VoiceInputDialog(
            voiceManager = viewModel.voiceManager,
            isMyanmar = isMyanmar,
            onDismiss = { viewModel.closeVoiceInputSheet() },
            onCommandCaptured = { title, category, dueTime, priority ->
                // Quick create task from recognized Burmese speech commands with extracted category & time
                viewModel.saveTodo(
                    id = 0L,
                    title = title,
                    notes = if (isMyanmar) "မြန်မာလို အသံအမိန့်ပေးချက်ဖြင့် ထည့်သွင်းထားသည်" else "Added via Burmese voice command",
                    category = category,
                    priority = priority,
                    dueDateMillis = dueTime,
                    reminderTimeMillis = dueTime,
                    repeatInterval = "NONE",
                    voiceNotePath = null
                )
            }
        )
    }

    // Category Manager Bottom Sheet
    if (isCategoryManagerOpen) {
        CategoryManagerDialog(
            categories = categories,
            isMyanmar = isMyanmar,
            onAddCategory = { viewModel.addCustomCategory(it) },
            onDeleteCategory = { viewModel.deleteCustomCategory(it) },
            onDismiss = { viewModel.closeCategoryManager() }
        )
    }

    // Offline Information Bottom Sheet
    if (isOfflineInfoSheetOpen) {
        OfflineInfoDialog(
            isOnline = isOnline,
            isMyanmar = isMyanmar,
            onDismiss = { viewModel.closeOfflineInfo() }
        )
    }
}

@Composable
fun StatsBannerCard(
    stats: TodoStats,
    isMyanmar: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isMyanmar) "ယနေ့ တိုးတက်မှု (Progress)" else "Daily Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (isMyanmar)
                            "ပြီးစီး ${stats.completedCount} / စုစုပေါင်း ${stats.totalCount}"
                        else "${stats.completedCount} of ${stats.totalCount} completed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${(stats.completionRate * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { stats.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun CategoryScrollRow(
    selectedCategory: String?,
    categories: List<CategoryEntity>,
    isMyanmar: Boolean,
    onCategorySelected: (String?) -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryList = if (categories.isNotEmpty()) {
        listOf(if (isMyanmar) "အားလုံး" else "All") + categories.map { if (isMyanmar) it.nameMm else it.name }
    } else {
        if (isMyanmar) {
            listOf("အားလုံး", "အထွေထွေ", "အလုပ်", "ကိုယ်ရေး", "ဈေးဝယ်", "ကျန်းမာရေး", "ပညာရေး")
        } else {
            listOf("All", "General", "Work", "Personal", "Shopping", "Health", "Study")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Manage / Add Categories button chip
        Surface(
            onClick = onManageCategories,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.testTag("manage_categories_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMyanmar) "အမျိုးအစား စီမံရန်" else "+ Manage",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        categoryList.forEach { cat ->
            val isAll = cat == "အားလုံး" || cat == "All"
            val isSelected = if (isAll) selectedCategory == null else selectedCategory == cat

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(if (isAll) null else cat) },
                label = { Text(cat, fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("filter_cat_$cat")
            )
        }
    }
}

@Composable
fun EmptyStateView(
    selectedTab: FilterTab,
    isMyanmar: Boolean,
    onAddTask: () -> Unit,
    onVoiceAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .testTag("empty_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (selectedTab) {
                        FilterTab.COMPLETED -> Icons.Default.CheckCircle
                        else -> Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val emptyTitle = when (selectedTab) {
            FilterTab.COMPLETED -> if (isMyanmar) "ပြီးစီးသော အလုပ်များ မရှိသေးပါ" else "No completed tasks yet"
            FilterTab.TODAY -> if (isMyanmar) "ဒီကနေ့အတွက် လုပ်ဆောင်ရန် မရှိပါ" else "No tasks for today"
            FilterTab.UPCOMING -> if (isMyanmar) "လာမည့်ရက်အတွက် သတိပေးချက် မရှိပါ" else "No upcoming reminders"
            FilterTab.ALL -> if (isMyanmar) "လုပ်ဆောင်ရန် မရှိသေးပါ" else "No tasks yet"
        }

        val emptySubtitle = if (isMyanmar) {
            "အလုပ်အသစ် သို့မဟုတ် မြန်မာလို အသံဖြင့် ချက်ချင်းထည့်သွင်းနိုင်ပါသည်"
        } else {
            "Add a new task or speak in Burmese to get started"
        }

        Text(
            text = emptyTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = emptySubtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExtendedFloatingActionButton(
                onClick = onVoiceAdd,
                icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                text = { Text(if (isMyanmar) "မြန်မာလို အသံဖြင့်ပြောမည်" else "Speak Burmese") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
