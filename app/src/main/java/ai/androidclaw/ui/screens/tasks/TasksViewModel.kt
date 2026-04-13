package ai.androidclaw.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.Task
import ai.androidclaw.domain.model.TaskStatus
import ai.androidclaw.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * 任务列表 ViewModel
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    init {
        loadTasks()
    }
    
    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                _uiState.value = _uiState.value.copy(
                    allTasks = tasks,
                    filteredTasks = filterTasks(tasks, _uiState.value.selectedFilter)
                )
            }
        }
    }
    
    fun setFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredTasks = filterTasks(_uiState.value.allTasks, filter)
        )
    }
    
    private fun filterTasks(tasks: List<Task>, filter: TaskFilter): List<Task> {
        return when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.TODO -> tasks.filter { it.status == TaskStatus.TODO }
            TaskFilter.IN_PROGRESS -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            TaskFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            TaskFilter.AWAITING -> tasks.filter { it.status == TaskStatus.AWAITING_HUMAN_INPUT }
        }
    }
    
    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, status)
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}

/**
 * 任务过滤选项
 */
enum class TaskFilter {
    ALL,
    TODO,
    IN_PROGRESS,
    COMPLETED,
    AWAITING
}

/**
 * 任务列表 UI 状态
 */
data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)
