package ai.androidclaw.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.androidclaw.domain.model.Task
import ai.androidclaw.domain.model.TaskStatus
import ai.androidclaw.domain.model.reminder.Reminder
import ai.androidclaw.domain.model.reminder.ReminderStatus
import ai.androidclaw.domain.model.reminder.ReminderType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * 任务列表界面（包含提醒管理）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    reminderViewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminderUiState by reminderViewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Reminders") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        // 跳转到聊天界面创建任务
                    } else {
                        showAddReminderDialog = true
                    }
                }
            ) {
                Icon(
                    if (selectedTab == 0) Icons.Default.Add else Icons.Default.AlarmAdd,
                    contentDescription = "Add"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tasks") },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reminders") },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                )
            }
            
            when (selectedTab) {
                0 -> TasksTab(uiState, viewModel)
                1 -> RemindersTab(
                    reminderUiState,
                    reminderViewModel,
                    onAddReminder = { showAddReminderDialog = true }
                )
            }
        }
    }
    
    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { title, description, type, date, hour, minute, repeatMinutes ->
                reminderViewModel.createReminder(
                    title = title,
                    description = description,
                    type = type,
                    date = date,
                    hour = hour,
                    minute = minute,
                    repeatIntervalMinutes = repeatMinutes
                )
                showAddReminderDialog = false
            }
        )
    }
}

@Composable
fun TasksTab(
    uiState: TasksUiState,
    viewModel: TasksViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 过滤标签
        FilterChips(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = { viewModel.setFilter(it) }
        )
        
        if (uiState.filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredTasks) { task ->
                    TaskItem(
                        task = task,
                        onStatusChange = { status ->
                            viewModel.updateTaskStatus(task.id, status)
                        },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RemindersTab(
    uiState: RemindersUiState,
    viewModel: RemindersViewModel,
    onAddReminder: () -> Unit
) {
    if (uiState.reminders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No reminders",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add a reminder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAddReminder) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Reminder")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.reminders) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onToggle = { viewModel.toggleReminder(reminder) },
                    onDelete = { viewModel.deleteReminder(reminder.id) }
                )
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = when (reminder.type) {
                                    ReminderType.ONCE -> "Once"
                                    ReminderType.DAILY -> "Daily"
                                    ReminderType.WEEKLY -> "Weekly"
                                    ReminderType.CUSTOM -> "Custom"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                when (reminder.type) {
                                    ReminderType.ONCE -> Icons.Default.Notifications
                                    ReminderType.DAILY -> Icons.Default.Repeat
                                    ReminderType.WEEKLY -> Icons.Default.DateRange
                                    ReminderType.CUSTOM -> Icons.Default.Schedule
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    
                    Text(
                        text = formatReminderTime(reminder),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 启用/禁用开关
            Switch(
                checked = reminder.status == ReminderStatus.ACTIVE,
                onCheckedChange = { onToggle() }
            )
            
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete '${reminder.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        type: ReminderType,
        date: LocalDate?,
        hour: Int,
        minute: Int,
        repeatIntervalMinutes: Long?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReminderType.ONCE) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var hour by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(0) }
    var repeatInterval by remember { mutableStateOf("60") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                // 提醒类型
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == ReminderType.ONCE,
                        onClick = { selectedType = ReminderType.ONCE },
                        label = { Text("Once") }
                    )
                    FilterChip(
                        selected = selectedType == ReminderType.DAILY,
                        onClick = { selectedType = ReminderType.DAILY },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = selectedType == ReminderType.WEEKLY,
                        onClick = { selectedType = ReminderType.WEEKLY },
                        label = { Text("Weekly") }
                    )
                }
                
                // 日期选择（仅一次性提醒）
                if (selectedType == ReminderType.ONCE) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDate.toString())
                    }
                }
                
                // 时间选择
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format("%02d:%02d", hour, minute))
                }
                
                // 重复间隔（仅自定义提醒）
                if (selectedType == ReminderType.CUSTOM) {
                    OutlinedTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it },
                        label = { Text("Repeat interval (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intervalMinutes = if (selectedType == ReminderType.CUSTOM) {
                        repeatInterval.toLongOrNull() ?: 60
                    } else null
                    
                    onConfirm(
                        title,
                        description,
                        selectedType,
                        if (selectedType == ReminderType.ONCE) selectedDate else null,
                        hour,
                        minute,
                        intervalMinutes
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FilterChips(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.name.replace("_", " ")) }
            )
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = task.status.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    
                    Text(
                        text = formatDate(task.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    TaskStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text("Set ${status.name.replace("_", " ")}") },
                            onClick = {
                                onStatusChange(status)
                                showMenu = false
                            },
                            enabled = status != task.status
                        )
                    }
                    
                    HorizontalDivider()
                    
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun formatReminderTime(reminder: Reminder): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
    return when (reminder.type) {
        ReminderType.ONCE -> formatter.format(reminder.scheduledAt)
        ReminderType.DAILY -> "Daily at ${reminder.scheduledAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
        ReminderType.WEEKLY -> "Weekly"
        ReminderType.CUSTOM -> "Every ${reminder.repeatIntervalMinutes ?: 60} min"
    }
}
