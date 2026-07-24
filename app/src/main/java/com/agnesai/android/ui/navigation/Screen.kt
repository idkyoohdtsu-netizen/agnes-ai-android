package com.agnesai.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Chat : Screen(
        route = "chat",
        title = "Chat",
        icon = Icons.Filled.Chat
    )

    object Files : Screen(
        route = "files",
        title = "Files",
        icon = Icons.Filled.Folder
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Filled.Settings
    )
}
