package com.corcozalex.tasklock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.corcozalex.tasklock.network.COMMON_REQUIRED_OBJECTS
import com.corcozalex.tasklock.network.RepeatMode
import com.corcozalex.tasklock.network.Task
import com.corcozalex.tasklock.network.TaskMetadataCodec
import com.corcozalex.tasklock.ui.components.CreateTaskDialog
import com.corcozalex.tasklock.ui.components.TaskCard
import com.corcozalex.tasklock.viewmodel.DashboardState
import java.util.Calendar

private enum class DashboardTab {
    SETTINGS,
    TASKS,
    ACCOUNT
}

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onLogoutClick: () -> Unit,
    onCreateTaskClick: (String, String, Long, RepeatMode, Int?, String) -> Unit,
    onUpdateTaskClick: (Task, String, String, Long, RepeatMode, Int?, String) -> Unit,
    onToggleTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (Int) -> Unit,
    onSyncTaskAlarms: (List<Task>) -> Unit,
    onTestAlarmClick: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var selectedTab by remember { mutableStateOf(DashboardTab.TASKS) }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 0.dp, shadowElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TaskLock",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (uiState is DashboardState.Success) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        showCreateDialog = true
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        },
        bottomBar = {
            if (uiState is DashboardState.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavAction(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            selected = selectedTab == DashboardTab.SETTINGS,
                            onClick = { selectedTab = DashboardTab.SETTINGS }
                        )

                        BottomNavAction(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Home,
                            label = "Home",
                            selected = selectedTab == DashboardTab.TASKS,
                            onClick = { selectedTab = DashboardTab.TASKS }
                        )

                        BottomNavAction(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Person,
                            label = "Account",
                            selected = selectedTab == DashboardTab.ACCOUNT,
                            onClick = { selectedTab = DashboardTab.ACCOUNT }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading your tasks...")
                        }
                    }
                }

                is DashboardState.Success -> {
                    val profile = state.userProfile
                    val tasks = state.tasks

                    LaunchedEffect(tasks) {
                        onSyncTaskAlarms(tasks)
                    }

                    when (selectedTab) {
                        DashboardTab.TASKS -> {
                            if (tasks.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No tasks yet. Tap + to add one.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    items(tasks) { task ->
                                        TaskCard(
                                            task = task,
                                            onToggleEnabled = { onToggleTaskClick(task) },
                                            onEditClick = {
                                                editingTask = task
                                                showCreateDialog = true
                                            },
                                            onDeleteClick = { onDeleteTaskClick(task.id) }
                                        )
                                    }
                                }
                            }
                        }

                        DashboardTab.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DashboardSectionCard {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Alarm tools",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = onTestAlarmClick) {
                                            Text("Test Alarm (15s)")
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.ACCOUNT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                Text(
                                    text = "Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DashboardSectionCard {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = profile.email,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(onClick = onLogoutClick) {
                                            Text("Logout")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is DashboardState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Security Error: ${state.error}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLogoutClick) {
                            Text("Clear Session & Login Again")
                        }
                    }
                }
            }

            if (showCreateDialog) {
                val taskToEdit = editingTask
                val metadata = taskToEdit?.let { TaskMetadataCodec.decode(it.description) }
                CreateTaskDialog(
                    onDismiss = {
                        showCreateDialog = false
                        editingTask = null
                    },
                    dialogTitle = if (taskToEdit == null) "New Task" else "Edit Task",
                    confirmLabel = if (taskToEdit == null) "Save" else "Update",
                    initialTitle = taskToEdit?.title.orEmpty(),
                    initialDescription = metadata?.notes
                        ?: TaskMetadataCodec.plainDescription(taskToEdit?.description).orEmpty(),
                    initialScheduledAtMillis = metadata?.scheduledAtMillis,
                    initialRepeatMode = metadata?.repeatMode ?: RepeatMode.NONE,
                    initialRepeatDayOfWeek = metadata?.repeatDayOfWeek
                        ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
                    initialRequiredObject = metadata?.requiredObject ?: (COMMON_REQUIRED_OBJECTS.firstOrNull() ?: "Object"),
                    onConfirm = { title, description, scheduledAtMillis, repeatMode, repeatDayOfWeek, requiredObject ->
                        showCreateDialog = false
                        if (taskToEdit == null) {
                            onCreateTaskClick(
                                title,
                                description,
                                scheduledAtMillis,
                                repeatMode,
                                repeatDayOfWeek,
                                requiredObject
                            )
                        } else {
                            onUpdateTaskClick(
                                taskToEdit,
                                title,
                                description,
                                scheduledAtMillis,
                                repeatMode,
                                repeatDayOfWeek,
                                requiredObject
                            )
                        }
                        editingTask = null
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = { Column(content = content) }
    )
}

@Composable
private fun BottomNavAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

