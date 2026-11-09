package com.kiri.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kiri.feature.auth.presentation.AuthRoute
import com.kiri.feature.onboarding.presentation.OnboardingRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val ROUTE_AUTH = "auth"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_MAIN = "main"

/**
 * @param isLoggedIn 저장된 로그인 토큰이 있는지. 메인에 있는 동안 false 가 되면(토큰 재발급 거절) 로그인으로 보낸다
 */
@Composable
fun KiriApp(isLoggedIn: Flow<Boolean> = emptyFlow(), initialRoute: String = ROUTE_AUTH) {
    val rootNavController = rememberNavController()
    // 처음 값을 읽기 전(null)에는 아무것도 하지 않는다
    val loggedIn by isLoggedIn.collectAsState(initial = null)
    val currentRoute = rootNavController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(loggedIn, currentRoute) {
        if (loggedIn == false && currentRoute == ROUTE_MAIN) rootNavController.navigateToLoginFromMain()
    }

    NavHost(navController = rootNavController, startDestination = initialRoute) {
        composable(ROUTE_AUTH) {
            AuthRoute(
                // 로그인과 온보딩은 하나의 진입 플로우라 로그인을 백스택에 남겨둔다
                onNavigateToOnboarding = { rootNavController.navigate(ROUTE_ONBOARDING) },
                // 약관에 동의한 기존 회원은 온보딩 없이 메인으로 가고, 로그인으로 뒤로가지 않게 지운다
                onNavigateToMain = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(rootNavController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(
                // 온보딩을 마치면 로그인·온보딩으로 뒤로가지 않도록 백스택을 비우고, 중복 이동은 막는다
                onNavigateToMainFlow = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(rootNavController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBrowseClick = {
                    rootNavController.navigate(ROUTE_MAIN) {
                        popUpTo(rootNavController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_MAIN) {
            MainScreen(
                // 로그아웃·회원탈퇴 뒤에는 메인으로 뒤로가지 않도록 메인을 지우고 로그인으로 간다
                onNavigateToLogin = { rootNavController.navigateToLoginFromMain() },
            )
        }
    }
}

// 메인으로 뒤로가지 않도록 메인을 지우고 로그인으로 간다
private fun NavHostController.navigateToLoginFromMain() {
    navigate(ROUTE_AUTH) {
        popUpTo(ROUTE_MAIN) { inclusive = true }
        launchSingleTop = true
    }
}
