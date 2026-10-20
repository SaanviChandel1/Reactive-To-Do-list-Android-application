package com.example.todo.model

/**
 * Priority levels for a Task.
 */
enum class Priority {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromString(value: String?): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
