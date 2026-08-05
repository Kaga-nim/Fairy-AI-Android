package com.kaganim.fairyai.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    // Main Features
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Notes : Screen("notes", "Notes", Icons.Default.Notes)
    object Todo : Screen("todo", "Todo", Icons.Default.Checklist)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    
    // Sub-screens
    object NoteDetail : Screen("note_detail", "Note Detail")
    
    // Auth
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Register", Icons.Default.Lock)
    
    companion object {
        val bottomNavItems: List<Screen> get() = listOf(Chat, Notes, Todo, Profile)
    }
}
