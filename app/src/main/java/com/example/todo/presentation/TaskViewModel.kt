package com.example.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.repository.TaskRepositoryImpl
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
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

    protected val _rawTasks = MutableStateFlow<List<Task>>(emptyList())
    protected val _errorMessage = MutableStateFlow<String?>(null)

    init {
        observeTaskStream()
    }

    /**
     * Observes real-time updates from Firebase Realtime Database using Kotlin Coroutines.
     * Collects emissions from the repository's callbackFlow stream within viewModelScope.
     */
    private fun observeTaskStream() {
        viewModelScope.launch {
            repository.getTasks()
                .catch { throwable ->
                    _errorMessage.value = throwable.localizedMessage ?: "Failed to stream tasks from Firebase"
                }
                .collect { tasks ->
                    _rawTasks.value = tasks
                }
        }
    }
}
