package com.kaganim.fairyai.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.kaganim.fairyai.presentation.features.chat.ChatScreen
import com.kaganim.fairyai.presentation.features.notes.NotesScreen
import com.kaganim.fairyai.presentation.features.notes.NoteDetailScreen
import com.kaganim.fairyai.presentation.features.todo.TodoScreen
import com.kaganim.fairyai.presentation.features.profile.ProfileScreen
import com.kaganim.fairyai.presentation.features.auth.LoginScreen
import com.kaganim.fairyai.presentation.features.auth.RegisterScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Chat.route) {
            ChatScreen()
        }
        composable(Screen.Notes.route) {
            NotesScreen(
                onAddNote = {
                    navController.navigate("${Screen.NoteDetail.route}/-1")
                },
                onEditNote = { noteId ->
                    navController.navigate("${Screen.NoteDetail.route}/$noteId")
                }
            )
        }
        composable(
            route = "${Screen.NoteDetail.route}/{noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            NoteDetailScreen(
                onSaveSuccess = {
                    navController.navigateUp()
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.Todo.route) {
            TodoScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
