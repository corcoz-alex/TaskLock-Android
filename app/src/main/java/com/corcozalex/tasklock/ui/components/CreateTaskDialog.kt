package com.corcozalex.tasklock.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.corcozalex.tasklock.network.COMMON_REQUIRED_OBJECTS
import com.corcozalex.tasklock.network.RepeatMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, RepeatMode, Int?, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var repeatMode by remember { mutableStateOf(RepeatMode.NONE) }
    var repeatDayOfWeek by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    var selectedObject by remember { mutableStateOf(COMMON_REQUIRED_OBJECTS.first()) }
    var objectMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    val selectedDateTime = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var selectedDateTimeMillis by remember { mutableStateOf(selectedDateTime.timeInMillis) }

    val dateLabel = remember(selectedDateTimeMillis) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            .format(selectedDateTimeMillis)
    }
    val timeLabel = remember(selectedDateTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(selectedDateTimeMillis)
    }

    val dayNameMap = remember {
        mapOf(
            Calendar.SUNDAY to "Sunday",
            Calendar.MONDAY to "Monday",
            Calendar.TUESDAY to "Tuesday",
            Calendar.WEDNESDAY to "Wednesday",
            Calendar.THURSDAY to "Thursday",
            Calendar.FRIDAY to "Friday",
            Calendar.SATURDAY to "Saturday"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateTimeMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = selectedDateTimeMillis
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDateTimeMillis = updated.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Date: $dateLabel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateTimeMillis }
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = selectedDateTimeMillis
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDateTimeMillis = updated.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Time: $timeLabel")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = repeatMode == RepeatMode.NONE,
                        onClick = { repeatMode = RepeatMode.NONE },
                        label = { Text("Once") }
                    )
                    FilterChip(
                        selected = repeatMode == RepeatMode.DAILY,
                        onClick = { repeatMode = RepeatMode.DAILY },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = repeatMode == RepeatMode.WEEKLY,
                        onClick = { repeatMode = RepeatMode.WEEKLY },
                        label = { Text("Weekly") }
                    )
                }

                if (repeatMode == RepeatMode.WEEKLY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = dayMenuExpanded,
                        onExpandedChange = { dayMenuExpanded = !dayMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = dayNameMap[repeatDayOfWeek].orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repeats on") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clickable { dayMenuExpanded = true }
                        )
                        ExposedDropdownMenu(
                            expanded = dayMenuExpanded,
                            onDismissRequest = { dayMenuExpanded = false }
                        ) {
                            dayNameMap.forEach { (day, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        repeatDayOfWeek = day
                                        dayMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = objectMenuExpanded,
                    onExpandedChange = { objectMenuExpanded = !objectMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedObject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Required Object") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clickable { objectMenuExpanded = true }
                    )
                    ExposedDropdownMenu(
                        expanded = objectMenuExpanded,
                        onDismissRequest = { objectMenuExpanded = false }
                    ) {
                        COMMON_REQUIRED_OBJECTS.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    selectedObject = item
                                    objectMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val repeatDay = if (repeatMode == RepeatMode.WEEKLY) repeatDayOfWeek else null
                    onConfirm(
                        title.trim(),
                        description.trim(),
                        selectedDateTimeMillis,
                        repeatMode,
                        repeatDay,
                        selectedObject
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}