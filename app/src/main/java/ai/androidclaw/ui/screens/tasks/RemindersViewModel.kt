package ai.androidclaw.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.reminder.Reminder
import ai.androidclaw.domain.model.reminder.ReminderStatus
import ai.androidclaw.domain.model.reminder.ReminderType
import ai.androidclaw.domain.repository.ReminderRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import javax.inject.Inject

/**
 * 提醒 ViewModel
 */
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()
    
    init {
        loadReminders()
    }
    
    private fun loadReminders() {
        viewModelScope.launch {
            reminderRepository.getAllReminders().collect { reminders ->
                _uiState.value = _uiState.value.copy(
                    reminders = reminders.sortedBy { it.scheduledAt },
                    isLoading = false
                )
            }
        }
    }
    
    fun createReminder(
        title: String,
        description: String = "",
        type: ReminderType = ReminderType.ONCE,
        date: LocalDate? = null,
        hour: Int = 12,
        minute: Int = 0,
        repeatIntervalMinutes: Long? = null
    ) {
        viewModelScope.launch {
            val scheduledAt = when (type) {
                ReminderType.ONCE -> {
                    val localDate = date ?: LocalDate.now()
                    val localDateTime = LocalDateTime.of(localDate, LocalTime.of(hour, minute))
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                }
                ReminderType.DAILY -> {
                    val today = LocalDate.now()
                    val localDateTime = LocalDateTime.of(today, LocalTime.of(hour, minute))
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                }
                ReminderType.WEEKLY -> {
                    val today = LocalDate.now()
                    val localDateTime = LocalDateTime.of(today, LocalTime.of(hour, minute))
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                }
                ReminderType.CUSTOM -> {
                    Instant.now() // 立即开始重复
                }
            }
            
            val reminder = Reminder(
                title = title,
                description = description,
                type = type,
                scheduledAt = scheduledAt,
                repeatIntervalMinutes = repeatIntervalMinutes
            )
            
            reminderRepository.createReminder(reminder)
        }
    }
    
    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val newStatus = if (reminder.status == ReminderStatus.ACTIVE) {
                ReminderStatus.DISABLED
            } else {
                ReminderStatus.ACTIVE
            }
            reminderRepository.updateReminderStatus(reminder.id, newStatus)
        }
    }
    
    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
        }
    }
    
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder)
        }
    }
}

/**
 * 提醒 UI 状态
 */
data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
