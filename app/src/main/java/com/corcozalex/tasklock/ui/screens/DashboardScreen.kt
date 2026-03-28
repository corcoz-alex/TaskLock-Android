package com.corcozalex.tasklock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corcozalex.tasklock.network.RepeatMode
import com.corcozalex.tasklock.network.Task
import com.corcozalex.tasklock.ui.components.CreateTaskDialog
import com.corcozalex.tasklock.ui.components.TaskCard
import com.corcozalex.tasklock.viewmodel.DashboardState

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onLogoutClick: () -> Unit,
    onCreateTaskClick: (String, String, Long, RepeatMode, Int?, String) -> Unit,
    onToggleTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (Int) -> Unit,
    onTestAlarmClick: () -> Unit
) {
    // State to toggle the popup dialog
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (uiState is DashboardState.Success) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { paddingValues ->
        // Main Content Box
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
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

                        // Header Section
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TaskLock",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = profile.email,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = onTestAlarmClick,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Test Alarm (15s)")
                                }

                                Button(
                                    onClick = onLogoutClick,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Logout")
                                }
                            }
                        }

                        HorizontalDivider()

                        // Task Section
                        if (tasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                Text(
                                    text = "No tasks yet. Time to add some!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(tasks) { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleEnabled = { onToggleTaskClick(task) },
                                        onDeleteClick = { onDeleteTaskClick(task.id) }
                                    )
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
            }

            // The Pop-up Dialog Overlay
            if (showCreateDialog) {
                CreateTaskDialog(
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { title, description, scheduledAtMillis, repeatMode, repeatDayOfWeek, requiredObject ->
                        showCreateDialog = false
                        onCreateTaskClick(
                            title,
                            description,
                            scheduledAtMillis,
                            repeatMode,
                            repeatDayOfWeek,
                            requiredObject
                        )
                    }
                )
            }
        }
    }
}