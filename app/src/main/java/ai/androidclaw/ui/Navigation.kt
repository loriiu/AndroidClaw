package ai.androidclaw.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Chat(
        route = "chat",
        label = "Chat",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    ),
    Tasks(
        route = "tasks",
        label = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    ),
    Skills(
        route = "skills",
        label = "Skills",
        selectedIcon = Icons.Filled.Extension,
        unselectedIcon = Icons.Outlined.Extension
    ),
    Settings(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

/**
 * 导航路由
 */
object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val TASKS = "tasks"
    const val SKILLS = "skills"
    const val SETTINGS = "settings"
    const val SETTINGS_LLM = "settings_llm"
    const val SETTINGS_USER = "settings_user"
}
