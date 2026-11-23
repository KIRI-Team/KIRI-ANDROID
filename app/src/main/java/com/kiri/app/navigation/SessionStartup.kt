package com.kiri.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import com.kiri.uikit.color.KiriColor
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.kiri.feature.auth.presentation.SplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionStartupViewModel @Inject constructor(private val sessions: SessionRepository) : ViewModel() {
    private val state = MutableStateFlow<SessionDestination?>(null)
    val destination = state.asStateFlow()
    val error = MutableStateFlow(false)
    init { retry() }
    fun retry() { error.value = false; viewModelScope.launch {
        sessions.restore().onSuccess { state.value = it }.onFailure { error.value = true }
    } }
}
// 앱 시작: 로그인 상태를 확인하는 동안 스플래시를 로그인 버튼이 나타나기 직전까지 재생한다.
// 자동 로그인(메인·온보딩)이어도 스플래시를 끝까지 보여준 뒤 이동하고, 로그인이 필요하면 로그인 화면이 이어서 버튼을 띄운다
@Composable
fun SessionStartup(isLoggedIn: kotlinx.coroutines.flow.Flow<Boolean>, viewModel: SessionStartupViewModel = hiltViewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    if (destination == null || !splashFinished) {
        SplashScreen(onFinished = { splashFinished = true }, play = !splashFinished) {
            // 스플래시가 끝났는데도 확인이 끝나지 않았을 때만 버튼 자리에 진행 상태나 다시 시도를 보여준다
            if (splashFinished) Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (error) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("로그인 상태를 확인하지 못했어요.", color = KiriColor.Text.Primary)
                    TextButton(viewModel::retry) { Text("다시 시도", color = KiriColor.Text.Accent) }
                } else CircularProgressIndicator()
            }
        }
    } else KiriApp(
        isLoggedIn = isLoggedIn,
        initialRoute = when (destination) {
            SessionDestination.MAIN -> "main"
            SessionDestination.ONBOARDING -> "onboarding"
            else -> "auth"
        },
        continueFromSplash = true,
    )
}
