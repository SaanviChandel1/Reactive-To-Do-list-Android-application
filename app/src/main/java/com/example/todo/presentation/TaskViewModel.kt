package com.example.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.repository.TaskRepositoryImpl
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.model.Priority
import com.example.todo.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    protected val _sortedTasks = MutableStateFlow<List<Task>>(emptyList())
    protected val _errorMessage = MutableStateFlow<String?>(null)

    init {
        observeTaskStream()
    }

    /**
     * Observes real-time updates from Firebase Realtime Database using Kotlin Coroutines.
     * Applies dynamic priority sorting before updating internal state.
     */
    private fun observeTaskStream() {
        viewModelScope.launch {
            repository.getTasks()
                .catch { throwable ->
                    _errorMessage.value = throwable.localizedMessage ?: "Failed to stream tasks from Firebase"
                }
                .map { tasks -> tasks.sortByPriority() }
                .collect { sortedList ->
                    _sortedTasks.value = sortedList
                }
        }
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
