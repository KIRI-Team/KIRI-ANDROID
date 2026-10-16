package com.kiri.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.rememberNavController
import com.kiri.uikit.color.KiriColor

// onNavigateToLogin: 로그아웃·회원탈퇴 뒤 로그인 화면으로 간다
@Composable
fun MainScreen(onNavigateToLogin: () -> Unit = {}) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = KiriColor.Bg.Base,
        contentColor = KiriColor.Text.Strong,
        // Reserve the measured bar AND its outer spacing/system inset for every tab/detail.
        bottomBar = { KiriBottomBar(navController) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).testTag("main_content")) {
            KiriNavHost(navController, Modifier.fillMaxSize(), onNavigateToLogin = onNavigateToLogin)
        }
    }
}
