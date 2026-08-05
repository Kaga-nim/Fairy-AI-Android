package com.kaganim.fairyai.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kaganim.fairyai.presentation.features.chat.ChatScreen
import com.kaganim.fairyai.presentation.features.notes.NotesScreen
import com.kaganim.fairyai.presentation.features.todo.TodoScreen
import com.kaganim.fairyai.presentation.features.profile.ProfileScreen
import com.kaganim.fairyai.presentation.features.auth.LoginScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Chat.route) {
            ChatScreen()
        }
        composable(Screen.Notes.route) {
            NotesScreen()
        }
        composable(Screen.Todo.route) {
            TodoScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        composable(Screen.Login.route) {
            LoginScreen()
        }
    }
}
