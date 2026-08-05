package id.kaganim.fairyai.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import id.kaganim.fairyai.presentation.screen.chat.ChatScreen
import id.kaganim.fairyai.presentation.screen.notes.NotesScreen
import id.kaganim.fairyai.presentation.screen.profile.ProfileScreen
import id.kaganim.fairyai.presentation.screen.todo.TodoScreen

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
    }
}
