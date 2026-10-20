package com.example.todo.domain.repository

import com.example.todo.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level contract for Task data operations.
 * Clean architecture decouples UI and business logic from Firebase SDK details.
 */
interface TaskRepository {

    /**
     * Emits a reactive, real-time stream of all tasks whenever data changes in Firebase.
     */
    fun getTasks(): Flow<List<Task>>

    /**
     * Adds a new task to Firebase Realtime Database.
     *
     * @param task Task to be added. If task.id is blank, a unique key will be generated.
     * @return [Result] indicating success or containing the thrown exception.
     */
    suspend fun addTask(task: Task): Result<Unit>

    /**
     * Updates an existing task in Firebase Realtime Database.
     *
     * @param task Task containing updated fields with a valid task.id.
     * @return [Result] indicating success or containing the thrown exception.
     */
    suspend fun updateTask(task: Task): Result<Unit>

    /**
     * Deletes a task by its unique ID from Firebase Realtime Database.
     *
     * @param taskId Unique identifier of the task node to remove.
     * @return [Result] indicating success or containing the thrown exception.
     */
    suspend fun deleteTask(taskId: String): Result<Unit>
}
