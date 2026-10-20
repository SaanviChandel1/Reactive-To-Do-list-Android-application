package com.example.todo.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo.model.Priority

/**
 * Task creation input dialog/form allowing users to enter a description
 * and select a priority level (HIGH, MEDIUM, LOW) before saving to the database.
 */
@Composable
fun TaskCreationDialog(
    onDismiss: () -> Unit,
    onConfirm: (description: String, priority: Priority) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        showError = false
                    },
                    label = { Text("Task Description") },
                    placeholder = { Text("e.g., Complete Android assignment") },
                    isError = showError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4
                )
                if (showError) {
                    Text(
                        text = "Description cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority
                        val color = getPriorityColor(priority)

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = description.trim()
                    if (trimmed.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(trimmed, selectedPriority)
                    }
                }
            ) {
                Text(text = "Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
