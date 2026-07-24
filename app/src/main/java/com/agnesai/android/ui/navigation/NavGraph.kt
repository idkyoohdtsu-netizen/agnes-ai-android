package com.agnesai.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agnesai.android.ui.screens.chat.ChatScreen
import com.agnesai.android.ui.screens.files.FilesScreen
import com.agnesai.android.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = modifier
    ) {
        composable(Screen.Chat.route) {
            ChatScreen()
        }
        composable(Screen.Files.route) {
            FilesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
