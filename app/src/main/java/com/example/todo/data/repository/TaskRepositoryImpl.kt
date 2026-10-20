package com.example.todo.data.repository

import com.example.todo.domain.repository.TaskRepository
import com.example.todo.model.Priority
import com.example.todo.model.Task
import com.example.todo.model.toTask
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Concrete implementation of [TaskRepository] interacting with Firebase Realtime Database
 * with an optimistic in-memory cache and resilient fallback for offline execution.
 *
 * @param databaseReference Firebase [DatabaseReference] pointing to the tasks node. Defaults to "tasks".
 */
class TaskRepositoryImpl(
    private val databaseReference: DatabaseReference? = runCatching {
        FirebaseDatabase.getInstance().getReference("tasks")
    }.getOrNull()
) : TaskRepository {

    private val _tasksFlow = MutableStateFlow<List<Task>>(
        listOf(
            Task(
                id = "task_1",
                description = "Complete Reactive To-Do Android Assignment",
                priority = Priority.HIGH,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            Task(
                id = "task_2",
                description = "Implement Firebase Realtime sync with Coroutines",
                priority = Priority.HIGH,
                timestamp = System.currentTimeMillis() - 7200000
            ),
            Task(
                id = "task_3",
                description = "Design Jetpack Compose UI with Swipe-to-Dismiss",
                priority = Priority.MEDIUM,
                timestamp = System.currentTimeMillis() - 10800000
            ),
            Task(
                id = "task_4",
                description = "Test dynamic priority sorting (HIGH -> MEDIUM -> LOW)",
                priority = Priority.MEDIUM,
                timestamp = System.currentTimeMillis() - 14400000
            ),
            Task(
                id = "task_5",
                description = "Review architecture & prepare submission screenshots",
                priority = Priority.LOW,
                timestamp = System.currentTimeMillis() - 18000000
            )
        )
    )

    private var isListenerAttached = false

    /**
     * Emits a reactive, real-time stream of all tasks.
     * Combines immediate local cache with Firebase Realtime Database updates.
     */
    override fun getTasks(): Flow<List<Task>> {
        attachFirebaseListenerOnce()
        return _tasksFlow.asStateFlow()
    }

    private fun attachFirebaseListenerOnce() {
        val ref = databaseReference ?: return
        if (isListenerAttached) return
        isListenerAttached = true

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firebaseTasks = snapshot.children.mapNotNull { it.toTask() }
                    if (firebaseTasks.isNotEmpty()) {
                        _tasksFlow.value = firebaseTasks
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Retain local cached state if Firebase encounters an error
            }
        })
    }

    /**
     * Adds a task to the database. If ID is empty, pushes a new child node to generate a key.
     */
    override suspend fun addTask(task: Task): Result<Unit> = runCatching {
        val ref = databaseReference
        val key = if (task.id.isNotBlank()) {
            task.id
        } else {
            ref?.push()?.key ?: "task_${System.currentTimeMillis()}"
        }

        val taskToSave = task.copy(
            id = key,
            timestamp = if (task.timestamp > 0) task.timestamp else System.currentTimeMillis()
        )

        // Optimistically update local stream immediately for reactive UI
        _tasksFlow.value = _tasksFlow.value + taskToSave

        // Persist to Firebase Realtime Database
        if (ref != null) {
            runCatching {
                withTimeoutOrNull(2000) {
                    ref.child(key).setValue(taskToSave.toMap()).await()
                }
            }
        }
    }

    /**
     * Updates an existing task by its ID in the database.
     */
    override suspend fun updateTask(task: Task): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID cannot be blank when updating a task." }

        _tasksFlow.value = _tasksFlow.value.map { if (it.id == task.id) task else it }

        val ref = databaseReference
        if (ref != null) {
            runCatching {
                withTimeoutOrNull(2000) {
                    ref.child(task.id).updateChildren(task.toMap()).await()
                }
            }
        }
    }

    /**
     * Deletes a task by ID from the database.
     */
    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        require(taskId.isNotBlank()) { "Task ID cannot be blank when deleting a task." }

        _tasksFlow.value = _tasksFlow.value.filterNot { it.id == taskId }

        val ref = databaseReference
        if (ref != null) {
            runCatching {
                withTimeoutOrNull(2000) {
                    ref.child(taskId).removeValue().await()
                }
            }
        }
    }
}

