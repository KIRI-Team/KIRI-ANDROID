package com.kiri.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kiri.app.navigation.KiriApp
import com.kiri.domain.model.invite.InviteLink
import com.kiri.domain.repository.AuthRepository
import com.kiri.domain.repository.PendingInviteRepository
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
                KiriTheme { KiriApp(isLoggedIn = authRepository.isLoggedIn) }
            }
        }
        // 화면 회전 등으로 다시 만들어질 때는 같은 링크를 다시 저장하지 않는다
        if (savedInstanceState == null) saveInviteCode(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        saveInviteCode(intent)
    }

    // 초대 링크(https://{도메인}/invite/{code})로 열렸으면 코드를 보관한다
    // TODO(invite-accept): 수락 흐름이 정해지면 pendingInviteRepository.consume()으로 꺼내서 처리
    private fun saveInviteCode(intent: Intent?) {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        val code = InviteLink.parseCode(uri.scheme, uri.host, uri.path) ?: return
        pendingInviteRepository.save(code)
    }
}
