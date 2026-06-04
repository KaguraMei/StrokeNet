package aya.strokenet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用导航屏幕定义
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Control : Screen(
        route = "control",
        title = "控制",
        icon = Icons.Default.Home
    )
    
    object Presets : Screen(
        route = "presets",
        title = "预设",
        icon = Icons.Default.Star
    )
    
    object Mcp : Screen(
        route = "mcp",
        title = "MCP",
        icon = Icons.Default.Cloud
    )
    
    object Settings : Screen(
        route = "settings",
        title = "设置",
        icon = Icons.Default.Settings
    )
    
    companion object {
        val screens = listOf(Control, Presets, Mcp, Settings)
    }
}
