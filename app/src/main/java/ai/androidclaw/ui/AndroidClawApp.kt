package ai.androidclaw.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.androidclaw.ui.screens.chat.ChatScreen
import ai.androidclaw.ui.screens.mcp.McpScreen
import ai.androidclaw.ui.screens.onboarding.OnboardingScreen
import ai.androidclaw.ui.screens.settings.SettingsScreen
import ai.androidclaw.ui.screens.skills.SkillsScreen
import ai.androidclaw.ui.screens.tasks.TasksScreen

/**
 * 主应用入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidClawApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 检查是否需要显示引导页
    LaunchedEffect(uiState.isInitialized) {
        if (!uiState.isInitialized) {
            navController.navigate(NavRoutes.ONBOARDING) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    val showBottomBar = currentDestination?.route in BottomNavItem.entries.map { it.route }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar && uiState.isInitialized) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.isInitialized) NavRoutes.Chat else NavRoutes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        viewModel.setInitialized()
                        navController.navigate(NavRoutes.Chat) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(NavRoutes.Chat) {
                ChatScreen()
            }
            
            composable(NavRoutes.Tasks) {
                TasksScreen()
            }
            
            composable(NavRoutes.Skills) {
                SkillsScreen()
            }
            
            composable(NavRoutes.Settings) {
                SettingsScreen(
                    onNavigateToMcp = {
                        navController.navigate(NavRoutes.SETTINGS_MCP)
                    }
                )
            }
            
            composable(NavRoutes.SETTINGS_MCP) {
                McpScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
