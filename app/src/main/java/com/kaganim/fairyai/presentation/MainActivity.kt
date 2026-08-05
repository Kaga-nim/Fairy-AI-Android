package com.kaganim.fairyai.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kaganim.fairyai.presentation.navigation.AppNavigation
import com.kaganim.fairyai.presentation.navigation.BottomBar
import com.kaganim.fairyai.presentation.theme.FairyAITheme
import com.kaganim.fairyai.domain.usecase.IsUserLoggedInUseCase
import com.kaganim.fairyai.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var isUserLoggedInUseCase: IsUserLoggedInUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isLoggedIn = true // Bypass login for development
        val startDestination = if (isLoggedIn) Screen.Chat.route else Screen.Login.route
        
        setContent {
            FairyAITheme {
                MainScreen(startDestination)
            }
        }
    }
}

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            paddingValues = innerPadding,
            startDestination = startDestination
        )
    }
}
