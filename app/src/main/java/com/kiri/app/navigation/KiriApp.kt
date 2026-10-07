package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kiri.feature.auth.presentation.AuthScreen
import com.kiri.feature.onboarding.presentation.OnboardingRoute

private const val ROUTE_AUTH = "auth"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_MAIN = "main"

@Composable
fun KiriApp() {
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = ROUTE_AUTH) {
        composable(ROUTE_AUTH) {
            AuthScreen(
                // 로그인과 온보딩은 하나의 진입 플로우라 로그인을 백스택에 남겨둔다
                onNextClick = { rootNavController.navigate(ROUTE_ONBOARDING) }
            )
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(
                onBrowseClick = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_MAIN) {
            MainScreen()
        }
    }
}
