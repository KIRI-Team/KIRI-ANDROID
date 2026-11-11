package com.kiri.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.home.BROWSING_BLOCKED_MESSAGE
import com.kiri.domain.home.FamilyAccess
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.rememberNavController
import com.kiri.uikit.color.KiriColor

// onNavigateToLogin: 로그아웃·회원탈퇴 뒤 로그인 화면으로 간다
@Composable
fun MainScreen(onNavigateToLogin: () -> Unit = {}, accessViewModel: FamilyAccessViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // 둘러보기 중이면 가족 기능인 기록 탭을 막는다. 화면에 돌아올 때마다 상태를 다시 확인한다
    val access by accessViewModel.access.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { accessViewModel.refresh() }

    Scaffold(
        containerColor = KiriColor.Bg.Base,
        contentColor = KiriColor.Text.Strong,
        // Reserve the measured bar AND its outer spacing/system inset for every tab/detail.
        bottomBar = {
            KiriBottomBar(
                navController,
                isBlocked = { it.requiresFamily && access == FamilyAccess.BROWSING },
                // TODO: 제한 안내 UI 가 정해지면 바꾼다. 그전까지는 토스트로 알린다
                onBlocked = { Toast.makeText(context, BROWSING_BLOCKED_MESSAGE, Toast.LENGTH_SHORT).show() },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).testTag("main_content")) {
            KiriNavHost(navController, Modifier.fillMaxSize(), onNavigateToLogin = onNavigateToLogin)
        }
    }
}
