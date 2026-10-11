package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kiri.feature.auth.presentation.AuthRoute
import com.kiri.feature.onboarding.presentation.OnboardingRoute

private const val ROUTE_AUTH = "auth"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_MAIN = "main"

@Composable
fun KiriApp() {
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = ROUTE_AUTH) {
        composable(ROUTE_AUTH) {
            AuthRoute(
                // 로그인과 온보딩은 하나의 진입 플로우라 로그인을 백스택에 남겨둔다
                onNavigateToOnboarding = { rootNavController.navigate(ROUTE_ONBOARDING) }
            )
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(
                // 온보딩을 마치면 로그인·온보딩으로 뒤로가지 않도록 백스택을 비우고, 중복 이동은 막는다
                onNavigateToMainFlow = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBrowseClick = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_MAIN) {
            MainScreen(
                // 로그아웃·회원탈퇴 뒤에는 메인으로 뒤로가지 않도록 메인을 지우고 로그인으로 간다
                onNavigateToLogin = {
                    rootNavController.navigate(ROUTE_AUTH) {
                        popUpTo(ROUTE_MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
