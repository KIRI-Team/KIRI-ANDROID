package com.kiri.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.kiri.uikit.color.KiriColor
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
@Composable
fun SessionStartup(isLoggedIn: kotlinx.coroutines.flow.Flow<Boolean>, viewModel: SessionStartupViewModel = hiltViewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    if (destination == null) Box(Modifier.fillMaxSize().background(KiriColor.Bg.Base), contentAlignment = Alignment.Center) {
        if (error) Column { Text("로그인 상태를 확인하지 못했어요.", color = KiriColor.Text.Primary); TextButton(viewModel::retry) { Text("다시 시도", color = KiriColor.Text.Accent) } }
        else CircularProgressIndicator()
    } else KiriApp(isLoggedIn, when (destination) {
        SessionDestination.MAIN -> "main"
        SessionDestination.ONBOARDING -> "onboarding"
        else -> "auth"
    })
}
