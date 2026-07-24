package com.agnesai.android.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agnesai.android.ui.navigation.Screen
import com.agnesai.android.ui.theme.AccentPurple
import com.agnesai.android.ui.theme.DarkContainer
import com.agnesai.android.ui.theme.DarkSurface
import com.agnesai.android.ui.theme.SubtleText

@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String?
) {
    val items = listOf(Screen.Chat, Screen.Files, Screen.Settings)

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = AccentPurple,
        tonalElevation = 0.dp
    ) {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentPurple,
                    selectedTextColor = AccentPurple,
                    indicatorColor = DarkContainer,
                    unselectedIconColor = SubtleText,
                    unselectedTextColor = SubtleText
                )
            )
        }
    }
}
