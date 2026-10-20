package com.example.todo.model

import com.google.firebase.database.DataSnapshot

/**
 * Domain model representing a single To-Do task.
 *
 * @property id Unique identifier of the task (matches Firebase node key).
 * @property description Details/content of the task.
 * @property priority Task priority level (HIGH, MEDIUM, LOW).
 * @property timestamp Epoch millisecond timestamp when the task was created or updated.
 */
data class Task(
    val id: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the [Task] into a key-value Map suitable for Firebase Realtime Database
     * setValue() or updateChildren() operations.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "description" to description,
            "priority" to priority.name,
            "timestamp" to timestamp
        )
    }

    companion object {
        /**
         * Maps a Firebase Realtime Database [DataSnapshot] to a [Task] object.
         * Gracefully handles nulls and defaults.
         *
         * @param snapshot The Firebase [DataSnapshot] pointing to a task item.
         * @return Parsed [Task], or null if snapshot does not exist or has no valid key.
         */
        fun fromSnapshot(snapshot: DataSnapshot): Task? {
            if (!snapshot.exists()) return null

            val id = snapshot.child("id").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() }
                ?: snapshot.key
                ?: return null

            val description = snapshot.child("description").getValue(String::class.java).orEmpty()
            val priorityStr = snapshot.child("priority").getValue(String::class.java)
            val priority = Priority.fromString(priorityStr)
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                ?: System.currentTimeMillis()

            return Task(
                id = id,
                description = description,
                priority = priority,
                timestamp = timestamp
            )
        }
    }
}

/**
 * Idiomatic extension function on [DataSnapshot] to parse into a [Task].
 */
fun DataSnapshot.toTask(): Task? = Task.fromSnapshot(this)
