package com.kiri.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kiri.uikit.component.LocalKiriBottomBarInset
import com.kiri.feature.record.navigation.serverScheduleDetailRoute
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState

private val TAB_START_ROUTES = TopLevelDestination.entries.map { it.startRoute }.toSet()

// onNavigateToLogin: 로그아웃·회원탈퇴 뒤 로그인 화면으로 간다
@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit = {},
    accessViewModel: FamilyAccessViewModel = hiltViewModel(),
    onCreateFamily: () -> Unit = {},
    onJoinFamily: () -> Unit = {},
    scheduleInviteViewModel: ScheduleInviteViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var editingHomeAnswer by remember { mutableStateOf(false) }
    // 둘러보기 중이면 가족 기능인 기록 탭을 막는다. 화면에 돌아올 때마다 상태를 다시 확인한다
    val access by accessViewModel.access.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { accessViewModel.refresh() }
    // 초대 링크로 가족에 참여한 결과를 알린다
    LaunchedEffect(accessViewModel) {
        accessViewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 바텀 바는 탭으로 이동할 수 있는 화면(각 탭의 첫 화면)에서만 보인다. 홈 답변 작성 중에는 숨긴다
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = route in TAB_START_ROUTES && !editingHomeAnswer

    // 카카오톡 일정 초대장으로 열렸으면 일정 상세로 간다. 응답할 수 있으면(canRespond) 요청알림 화면을 먼저 보여주고,
    // 볼 수 없는 일정(다른 가족의 일정 등)이면 상세 조회가 실패해 내용을 보여주지 않는다
    // 내비게이션 그래프가 준비된 뒤(route 가 생긴 뒤)에 이동한다
    val invitedScheduleId by scheduleInviteViewModel.scheduleId.collectAsStateWithLifecycle()
    LaunchedEffect(invitedScheduleId, route != null) {
        val scheduleId = invitedScheduleId ?: return@LaunchedEffect
        if (route == null) return@LaunchedEffect
        scheduleInviteViewModel.opened(scheduleId)
        navController.navigate(serverScheduleDetailRoute(scheduleId, fromHome = true, respond = true)) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = KiriColor.Bg.Base,
        contentColor = KiriColor.Text.Strong,
        bottomBar = {
            if (showBottomBar) KiriBottomBar(
                navController,
                isBlocked = { it.requiresFamily && access == FamilyAccess.BROWSING },
                // TODO: 제한 안내 UI 가 정해지면 바꾼다. 그전까지는 토스트로 알린다
                onBlocked = { Toast.makeText(context, BROWSING_BLOCKED_MESSAGE, Toast.LENGTH_SHORT).show() },
            )
        },
    ) { padding ->
        // 바가 있으면 콘텐츠를 떠 있는 바 뒤까지 그리고, 가려지는 높이는 스크롤 끝 여백으로 넘긴다.
        // 바가 없는 화면은 시스템 내비게이션 바 위에서 끝낸다
        val overlayBar = showBottomBar
        val contentPadding = if (overlayBar) PaddingValues(top = padding.calculateTopPadding()) else padding
        CompositionLocalProvider(LocalKiriBottomBarInset provides if (overlayBar) padding.calculateBottomPadding() else 0.dp) {
            Box(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(padding).testTag("main_content")) {
                KiriNavHost(navController, Modifier.fillMaxSize(), onNavigateToLogin = onNavigateToLogin,
                    onCreateFamily = onCreateFamily, onJoinFamily = onJoinFamily,
                    onHomeAnswerEditingChanged = { editingHomeAnswer = it })
            }
        }
    }
}
