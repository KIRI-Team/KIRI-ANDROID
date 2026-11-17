package com.kiri.app.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 카카오톡 일정 초대장으로 열린 일정 id. MainActivity 가 링크를 받아 넣고, 메인 화면이 뜨면 꺼내 일정 상세로 간다.
// 로그인하지 않았으면 로그인 뒤 메인 화면에 들어올 때 연다. 앱 프로세스가 끝나면 사라진다
@Singleton
class PendingScheduleInvite @Inject constructor() {
    private val pending = MutableStateFlow<Long?>(null)
    val scheduleId: StateFlow<Long?> = pending.asStateFlow()

    fun save(scheduleId: Long) {
        pending.value = scheduleId
    }

    // 연 일정만 지운다. 그사이 다른 초대장으로 열렸으면 그 일정은 남긴다
    fun consume(scheduleId: Long) {
        pending.compareAndSet(scheduleId, null)
    }
}

@HiltViewModel
class ScheduleInviteViewModel @Inject constructor(private val pending: PendingScheduleInvite) : ViewModel() {
    val scheduleId: StateFlow<Long?> = pending.scheduleId

    fun opened(scheduleId: Long) = pending.consume(scheduleId)
}
