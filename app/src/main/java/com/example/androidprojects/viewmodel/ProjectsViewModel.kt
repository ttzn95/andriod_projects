package com.example.androidprojects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidprojects.data.SampleData
import com.example.androidprojects.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class ProjectsUiState(
    val projects: List<Project> = SampleData.sampleProjects,
    val snippets: List<CodeSnippet> = SampleData.snippetsList,
    val selectedCategory: ProjectCategory = ProjectCategory.ALL,
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val selectedProject: Project? = null,
    val showAddDialog: Boolean = false
) {
    val filteredProjects: List<Project>
        get() = projects.filter { project ->
            val matchesCategory = selectedCategory == ProjectCategory.ALL || project.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    project.title.contains(searchQuery, ignoreCase = true) ||
                    project.description.contains(searchQuery, ignoreCase = true) ||
                    project.techStack.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFav = !onlyFavorites || project.isFavorite
            matchesCategory && matchesSearch && matchesFav
        }

    val totalProjectsCount: Int get() = projects.size
    val completedProjectsCount: Int get() = projects.count { it.status == ProjectStatus.COMPLETED }
    val inProgressProjectsCount: Int get() = projects.count { it.status == ProjectStatus.IN_PROGRESS }
    val favoriteProjectsCount: Int get() = projects.count { it.isFavorite }
}

class ProjectsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ProjectCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun selectProject(project: Project?) {
        _uiState.update { it.copy(selectedProject = project) }
    }

    fun toggleFavorite(projectId: String) {
        _uiState.update { state ->
            val updated = state.projects.map { p ->
                if (p.id == projectId) p.copy(isFavorite = !p.isFavorite) else p
            }
            val currentSelected = if (state.selectedProject?.id == projectId) {
                state.selectedProject.copy(isFavorite = !state.selectedProject.isFavorite)
            } else state.selectedProject
            state.copy(projects = updated, selectedProject = currentSelected)
        }
    }

    fun updateProjectProgress(projectId: String, progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        val status = if (clamped >= 1f) ProjectStatus.COMPLETED else ProjectStatus.IN_PROGRESS
        _uiState.update { state ->
            val updated = state.projects.map { p ->
                if (p.id == projectId) p.copy(progress = clamped, status = status) else p
            }
            val currentSelected = if (state.selectedProject?.id == projectId) {
                state.selectedProject.copy(progress = clamped, status = status)
            } else state.selectedProject
            state.copy(projects = updated, selectedProject = currentSelected)
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun addProject(
        title: String,
        tagLine: String,
        description: String,
        category: ProjectCategory,
        techStack: List<String>,
        architecture: String
    ) {
        val newProject = Project(
            id = "proj-" + UUID.randomUUID().toString().take(8),
            title = title,
            tagLine = tagLine,
            description = description,
            category = category,
            status = ProjectStatus.IN_PROGRESS,
            progress = 0.1f,
            techStack = techStack,
            architecture = architecture,
            isFavorite = false,
            starCount = 1,
            features = listOf(
                ProjectFeature("Core Architecture Setup", "Base MVVM architecture with Kotlin Coroutines"),
                ProjectFeature("Compose UI Foundation", "Material 3 design layout & themes")
            ),
            snippets = emptyList(),
            interactiveDemoType = InteractiveDemoType.ANIMATED_COUNTER
        )
        _uiState.update { state ->
            state.copy(
                projects = listOf(newProject) + state.projects,
                showAddDialog = false
            )
        }
    }

    fun deleteProject(projectId: String) {
        _uiState.update { state ->
            state.copy(
                projects = state.projects.filterNot { it.id == projectId },
                selectedProject = if (state.selectedProject?.id == projectId) null else state.selectedProject
            )
        }
    }

    fun addSnippet(title: String, category: String, description: String, code: String) {
        val newSnippet = CodeSnippet(
            id = "snip-" + UUID.randomUUID().toString().take(8),
            title = title,
            category = category,
            description = description,
            code = code
        )
        _uiState.update { state ->
            state.copy(snippets = listOf(newSnippet) + state.snippets)
        }
    }
}
