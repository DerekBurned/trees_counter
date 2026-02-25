package com.example.trees_houses_counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trees_houses_counter.presentation.auth.AuthScreen
import com.example.trees_houses_counter.presentation.map.MapScreen
import com.example.trees_houses_counter.presentation.theme.Trees_houses_counterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Trees_houses_counterTheme() {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("map") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("map") {
            MapScreen(
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("map") { inclusive = true }
                    }
                }
            )
        }
    }
}
