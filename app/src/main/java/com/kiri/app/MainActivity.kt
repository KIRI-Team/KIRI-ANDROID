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
import com.kiri.domain.model.invite.InviteLink
import com.kiri.domain.repository.AuthRepository
import com.kiri.domain.repository.PendingInviteRepository
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var pendingInviteRepository: PendingInviteRepository

    @Inject lateinit var clock: java.time.Clock

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 화면 배경이 상태바/내비게이션 바 뒤까지 이어지도록 하고, 어두운 배경에 맞춰 바 아이콘은 밝게 표시한다
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                com.kiri.uikit.LocalKiriClock provides clock
            ) {
                KiriTheme { com.kiri.app.navigation.SessionStartup(isLoggedIn = authRepository.isLoggedIn) }
            }
        }
        // 화면 회전 등으로 다시 만들어질 때는 같은 링크를 다시 저장하지 않는다
        if (savedInstanceState == null) saveInviteToken(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        saveInviteToken(intent)
    }

    // 초대 링크(https://kiri.app/invite/{token})로 열렸으면 초대 토큰을 기기에 보관한다.
    // 로그인 -> 온보딩 도중 앱이 종료돼도 남아 있고, 가족 참여가 끝나면 지운다
    // TODO(invite-accept): 가족 참여 API 가 나오면 온보딩을 마치고 홈에 들어갈 때 pendingInviteRepository.get() 으로 꺼내
    //  참여를 요청하고, 성공하거나 더 쓸 수 없는 토큰이면 clear() 한다
    private fun saveInviteToken(intent: Intent?) {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        val token = InviteLink.parseToken(uri.scheme, uri.host, uri.path) ?: return
        // 저장 직후 화면이 닫혀도 쓰기가 끝나도록 취소하지 않는다
        lifecycleScope.launch { withContext(NonCancellable) { pendingInviteRepository.save(token) } }
    }
}
