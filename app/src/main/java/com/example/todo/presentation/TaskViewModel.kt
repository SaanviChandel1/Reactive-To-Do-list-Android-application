package com.example.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.repository.TaskRepositoryImpl
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.model.Priority
import com.example.todo.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * TaskViewModel managing state and operations for the To-Do application.
 *
 * Injects [TaskRepository] to provide dependency inversion and decouple
 * presentation logic from Firebase database operations.
 *
 * @property repository Repository providing data operations on Firebase Realtime Database.
 */
class TaskViewModel(
    private val repository: TaskRepository = TaskRepositoryImpl()
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    /**
     * Reactive StateFlow exposed directly to the UI.
     * Combines the real-time sorted task stream with UI error/success messages.
     * Survives configuration changes with a 5-second buffer via WhileSubscribed(5000).
     */
    val uiState: StateFlow<TaskUiState> = combine(
        repository.getTasks()
            .catch { throwable ->
                _errorMessage.value = throwable.localizedMessage ?: "Failed to stream tasks from Firebase"
                emit(emptyList())
            }
            .map { tasks -> tasks.sortByPriority() },
        _errorMessage,
        _userMessage
    ) { sortedTasks, error, userMsg ->
        TaskUiState(
            isLoading = false,
            tasks = sortedTasks,
            errorMessage = error,
            userMessage = userMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState(isLoading = true)
    )

    /**
     * Adds a new task with given description and priority.
     */
    fun addTask(description: String, priority: Priority = Priority.MEDIUM) {
        val trimmed = description.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "Task description cannot be empty."
            return
        }

        viewModelScope.launch {
            val newTask = Task(
                description = trimmed,
                priority = priority,
                timestamp = System.currentTimeMillis()
            )
            repository.addTask(newTask)
                .onSuccess { _userMessage.value = "Task created successfully." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to add task." }
        }
    }

    /**
     * Updates an existing task's description or priority.
     */
    fun updateTask(task: Task) {
        if (task.description.isBlank()) {
            _errorMessage.value = "Task description cannot be empty."
            return
        }

        viewModelScope.launch {
            repository.updateTask(task)
                .onSuccess { _userMessage.value = "Task updated." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to update task." }
        }
    }

    /**
     * Deletes a task by its unique ID.
     */
    fun deleteTask(taskId: String) {
        if (taskId.isBlank()) return

        viewModelScope.launch {
            repository.deleteTask(taskId)
                .onSuccess { _userMessage.value = "Task deleted." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to delete task." }
        }
    }

    /**
     * Clears error message once acknowledged by UI.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clears user feedback message once acknowledged by UI.
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Maps [Priority] to priority ranking weight:
     * HIGH -> 0, MEDIUM -> 1, LOW -> 2.
     */
    private fun Priority.toSortRank(): Int = when (this) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
    }

    /**
     * Dynamically sorts tasks by priority (HIGH -> MEDIUM -> LOW),
     * followed by latest timestamp descending.
     */
    private fun List<Task>.sortByPriority(): List<Task> {
        return sortedWith(
            compareBy<Task> { it.priority.toSortRank() }
                .thenByDescending { it.timestamp }
        )
    }
}
