package com.kaganim.fairyai.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // CRITICAL FIX: Ensure destination and route are NOT NULL before any logic
    val currentRoute = currentDestination?.route ?: return

    val bottomNavItems = Screen.bottomNavItems

    // Show bottom bar on main screens and auth screens, but NOT on detail screens
    val showBottomBar = currentRoute in listOf(
        Screen.Chat.route,
        Screen.Notes.route,
        Screen.Todo.route,
        Screen.Profile.route,
        Screen.Login.route,
        Screen.Register.route
    )

    if (showBottomBar) {
        NavigationBar {
            bottomNavItems.filterNotNull().forEach { screen ->
                val screenRoute = screen.route
                val isSelected = currentDestination.hierarchy.any { it.route == screenRoute }
                
                NavigationBarItem(
                    icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                    label = { Text(screen.title) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(screenRoute) {
                                popToStart(navController)
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun androidx.navigation.NavOptionsBuilder.popToStart(navController: NavHostController) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
