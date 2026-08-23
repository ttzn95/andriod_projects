package com.example.androidprojects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidprojects.ui.screens.*
import com.example.androidprojects.ui.theme.AndroidProjectsTheme
import com.example.androidprojects.viewmodel.ProjectsViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    PROJECTS("Projects", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_projects"),
    PLAYGROUND("Playground", Icons.Filled.Widgets, Icons.Outlined.Widgets, "tab_playground"),
    SNIPPETS("Snippets", Icons.Filled.Code, Icons.Outlined.Code, "tab_snippets")
}

class MainActivity : ComponentActivity() {
    private val viewModel: ProjectsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidProjectsTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.selectedProject != null) {
                        ProjectDetailScreen(
                            project = uiState.selectedProject!!,
                            onBack = { viewModel.selectProject(null) },
                            onToggleFavorite = { viewModel.toggleFavorite(uiState.selectedProject!!.id) },
                            onUpdateProgress = { p ->
                                viewModel.updateProjectProgress(uiState.selectedProject!!.id, p)
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("main_bottom_nav"),
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationTab.entries.forEach { tab ->
                                        val isSelected = currentTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title
                                                )
                                            },
                                            label = { Text(tab.title) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                                    NavigationTab.HOME -> HomeScreen(
                                        uiState = uiState,
                                        onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                                        onSelectProject = { p -> viewModel.selectProject(p) },
                                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                        onUpdateProgress = { id, p -> viewModel.updateProjectProgress(id, p) },
                                        onNavigateToProjects = { currentTab = NavigationTab.PROJECTS },
                                        onNavigateToPlayground = { currentTab = NavigationTab.PLAYGROUND },
                                        onNavigateToSnippets = { currentTab = NavigationTab.SNIPPETS },
                                        onOpenAddProject = { viewModel.openAddDialog() }
                                    )
                                    NavigationTab.PROJECTS -> ProjectsListScreen(
                                        uiState = uiState,
                                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                        onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                                        onToggleFavoritesFilter = { viewModel.toggleFavoritesFilter() },
                                        onSelectProject = { p -> viewModel.selectProject(p) },
                                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                        onUpdateProgress = { id, p -> viewModel.updateProjectProgress(id, p) },
                                        onDeleteProject = { id -> viewModel.deleteProject(id) },
                                        onOpenAddProject = { viewModel.openAddDialog() }
                                    )
                                    NavigationTab.PLAYGROUND -> InteractivePlaygroundScreen()
                                    NavigationTab.SNIPPETS -> SnippetsScreen(
                                        snippets = uiState.snippets,
                                        onAddSnippet = { title, cat, desc, code ->
                                            viewModel.addSnippet(title, cat, desc, code)
                                        }
                                    )
                                }
                            }

                            if (uiState.showAddDialog) {
                                AddProjectDialog(
                                    onDismiss = { viewModel.closeAddDialog() },
                                    onConfirm = { title, tagLine, desc, cat, tech, arch ->
                                        viewModel.addProject(title, tagLine, desc, cat, tech, arch)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
