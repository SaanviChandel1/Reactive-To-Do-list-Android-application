package com.example.todo.presentation

import androidx.lifecycle.ViewModel
import com.example.todo.data.repository.TaskRepositoryImpl
import com.example.todo.domain.repository.TaskRepository

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
    // Base ViewModel structure and dependency injection initialized
}
