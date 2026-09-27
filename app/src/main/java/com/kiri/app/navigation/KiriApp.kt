package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kiri.feature.auth.presentation.AuthScreen
import com.kiri.feature.onboarding.presentation.OnboardingScreen

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_AUTH = "auth"
private const val ROUTE_MAIN = "main"

@Composable
fun KiriApp() {
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = ROUTE_ONBOARDING) {
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                onNextClick = {
                    rootNavController.navigate(ROUTE_AUTH) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_AUTH) {
            AuthScreen(
                onNextClick = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_MAIN) {
            MainScreen()
        }
    }
}
