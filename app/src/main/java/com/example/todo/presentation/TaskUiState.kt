package com.example.todo.presentation

import com.example.todo.model.Task

/**
 * UI State for the reactive To-Do Screen.
 *
 * @property isLoading True when tasks are initially loading from Firebase.
 * @property tasks The real-time list of tasks currently synced from Firebase.
 * @property errorMessage Error message if an operation or subscription fails.
 * @property userMessage Informational message/toast for successful operations.
 */
data class TaskUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
    val userMessage: String? = null
)
