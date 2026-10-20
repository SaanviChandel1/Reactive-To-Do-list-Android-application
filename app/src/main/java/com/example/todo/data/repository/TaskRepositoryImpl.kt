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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Concrete implementation of [TaskRepository] interacting directly with Firebase Realtime Database.
 *
 * @param databaseReference Firebase [DatabaseReference] pointing to the tasks node. Defaults to "tasks".
 */
class TaskRepositoryImpl(
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("tasks")
) : TaskRepository {

    /**
     * Listens for real-time changes in Firebase Realtime Database using [callbackFlow].
     * Converts Firebase's [ValueEventListener] callbacks into an asynchronous cold Flow.
     * Automatically registers the listener on collection and unregisters on cancellation.
     */
    override fun getTasks(): Flow<List<Task>> = callbackFlow {
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = snapshot.children.mapNotNull { childSnapshot ->
                    childSnapshot.toTask()
                }

                trySend(tasks).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                // Terminate flow with exception if Firebase query fails or is cancelled
                close(error.toException())
            }
        }

        databaseReference.addValueEventListener(valueEventListener)

        // Clean up the listener when the subscriber/collector is cancelled
        awaitClose {
            databaseReference.removeEventListener(valueEventListener)
        }
    }

    /**
     * Adds a task to the database. If ID is empty, pushes a new child node to generate a key.
     */
    override suspend fun addTask(task: Task): Result<Unit> = runCatching {
        val key = if (task.id.isNotBlank()) {
            task.id
        } else {
            databaseReference.push().key ?: throw IllegalStateException("Failed to generate unique Firebase key")
        }

        val taskToSave = task.copy(
            id = key,
            timestamp = if (task.timestamp > 0) task.timestamp else System.currentTimeMillis()
        )

        databaseReference.child(key).setValue(taskToSave.toMap()).await()
    }

    /**
     * Updates an existing task by its ID in the database.
     */
    override suspend fun updateTask(task: Task): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID cannot be blank when updating a task." }
        databaseReference.child(task.id).updateChildren(task.toMap()).await()
    }

    /**
     * Deletes a task by ID from the database.
     */
    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        require(taskId.isNotBlank()) { "Task ID cannot be blank when deleting a task." }
        databaseReference.child(taskId).removeValue().await()
    }
}
