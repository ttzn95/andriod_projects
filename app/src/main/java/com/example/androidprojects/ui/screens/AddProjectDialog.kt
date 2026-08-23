package com.example.androidprojects.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.androidprojects.model.ProjectCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        tagLine: String,
        description: String,
        category: ProjectCategory,
        techStack: List<String>,
        architecture: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var tagLine by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProjectCategory.ECOMMERCE) }
    var techStackText by remember { mutableStateOf("Jetpack Compose, Coroutines, Room") }
    var architecture by remember { mutableStateOf("MVVM + Clean Architecture") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Android Project",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g. Velocity Fitness Tracker") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tagLine,
                    onValueChange = { tagLine = it },
                    label = { Text("Short Tagline") },
                    placeholder = { Text("e.g. Real-time run tracking with GPS") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tagline_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        ProjectCategory.entries.filterNot { it == ProjectCategory.ALL }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = techStackText,
                    onValueChange = { techStackText = it },
                    label = { Text("Tech Stack (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = architecture,
                    onValueChange = { architecture = it },
                    label = { Text("Architecture Pattern") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Explain core modules, key features, and architecture goals...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val stackList = techStackText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onConfirm(
                            title.trim(),
                            if (tagLine.isBlank()) title else tagLine.trim(),
                            if (description.isBlank()) "Android application created with modern Compose architecture." else description.trim(),
                            selectedCategory,
                            if (stackList.isEmpty()) listOf("Jetpack Compose", "Kotlin") else stackList,
                            if (architecture.isBlank()) "MVVM" else architecture.trim()
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("submit_project_button")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_project_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
