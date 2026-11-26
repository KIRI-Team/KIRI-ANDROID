package com.kiri.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.kiri.app.navigation.KiriApp
import com.kiri.app.push.NotificationPermissionPromptEnabled
import com.kiri.app.navigation.PendingScheduleInvite
import com.kiri.domain.invite.model.InviteLink
import com.kiri.domain.schedule.model.ScheduleInviteLink
import com.kiri.domain.auth.repository.AuthRepository
import com.kiri.domain.invite.repository.PendingInviteRepository
import com.kiri.uikit.KiriTheme
import com.kiri.uikit.permission.LocalNotificationPermissionPromptEnabled
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var pendingInviteRepository: PendingInviteRepository

    @Inject lateinit var pendingScheduleInvite: PendingScheduleInvite

    @Inject lateinit var clock: java.time.Clock

    @Inject
    lateinit var authRepository: AuthRepository

    @JvmField
    @Inject
    @NotificationPermissionPromptEnabled
    var notificationPermissionPromptEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 화면 배경이 상태바/내비게이션 바 뒤까지 이어지도록 하고, 어두운 배경에 맞춰 바 아이콘은 밝게 표시한다
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                com.kiri.uikit.LocalKiriClock provides clock,
                LocalNotificationPermissionPromptEnabled provides notificationPermissionPromptEnabled,
            ) {
                KiriTheme { com.kiri.app.navigation.SessionStartup(isLoggedIn = authRepository.isLoggedIn) }
            }
        }
        // 화면 회전 등으로 다시 만들어질 때는 같은 링크를 다시 저장하지 않는다
        if (savedInstanceState == null) handleLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent?) {
        saveInviteToken(intent)
        saveScheduleInvite(intent)
    }

    // 카카오톡 일정 초대장 버튼(kakao{앱 키}://kakaolink?scheduleId={id})으로 열렸으면 일정 id 를 넘긴다.
    // 메인 화면이 일정 상세를 불러오고, 응답할 수 있으면(canRespond) 요청알림 화면을 먼저 보여준다
    private fun saveScheduleInvite(intent: Intent?) {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        val scheduleId = ScheduleInviteLink.parseScheduleId(
            uri.scheme, uri.host, uri.getQueryParameter(ScheduleInviteLink.SCHEDULE_ID_PARAM),
        ) ?: return
        pendingScheduleInvite.save(scheduleId)
    }

    // 초대 링크(https://halo-official.co.kr/invite/{token})로 열렸으면 초대 토큰을 기기에 보관한다.
    // 로그인 -> 온보딩 도중 앱이 종료돼도 남아 있고, 가족 참여가 끝나면 지운다
    // 보관한 토큰은 AcceptPendingInviteUseCase 가 꺼내 참여한다: 온보딩은 가족 선택 화면에 들어올 때,
    // 메인은 화면에 돌아올 때(FamilyAccessViewModel). 참여했거나 더 쓸 수 없는 토큰이면 지운다
    private fun saveInviteToken(intent: Intent?) {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        val token = InviteLink.parseToken(uri.scheme, uri.host, uri.path) ?: return
        // 저장 직후 화면이 닫혀도 쓰기가 끝나도록 취소하지 않는다
        lifecycleScope.launch { withContext(NonCancellable) { pendingInviteRepository.save(token) } }
    }
}
