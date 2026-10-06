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
            // TODO: 온보딩 완료 시점이 정해지면 ROUTE_MAIN으로 이동 연결 (popUpTo(ROUTE_AUTH) { inclusive = true })
            OnboardingRoute()
        }
        composable(ROUTE_MAIN) {
            MainScreen()
        }
    }
}
