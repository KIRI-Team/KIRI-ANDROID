package com.kiri.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.FamilyAccess
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.access
import com.kiri.domain.usecase.invite.AcceptPendingInviteUseCase
import com.kiri.domain.usecase.invite.InviteAcceptResult
import com.kiri.domain.usecase.invite.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// 메인 화면에서 지금 사용자의 가족 이용 상태(방장·참여자·둘러보기)를 들고 있다. 하단 탭에서 둘러보기 사용자를 막는 데 쓴다
// 가족 정보(HomeRepository.family)는 잠깐 재사용되므로 화면에 돌아올 때마다 불러도 요청이 쌓이지 않는다
// 초대 링크로 받아 둔 토큰이 있으면 먼저 그 가족에 참여한다 (온보딩 없이 메인으로 온 기존 회원, 둘러보기 사용자)
@HiltViewModel
class FamilyAccessViewModel @Inject constructor(
    private val home: HomeRepository,
    private val acceptInvite: AcceptPendingInviteUseCase? = null,
) : ViewModel() {
    // 아직 모르면 null. 모를 때는 막지 않는다 (네트워크 실패로 가족 기능을 잘못 막지 않도록)
    private val _access = MutableStateFlow<FamilyAccess?>(null)
    val access: StateFlow<FamilyAccess?> = _access.asStateFlow()

    // 초대 링크로 참여한 결과 안내 (토스트)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    // 화면에 빠르게 여러 번 돌아와도 같은 토큰으로 두 번 참여하지 않도록 한 번에 하나씩 확인한다
    private val refreshing = Mutex()

    fun refresh() {
        viewModelScope.launch {
            refreshing.withLock {
                acceptPendingInvite()
                try {
                    _access.value = home.family().access
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 불러오지 못하면 알고 있던 상태를 그대로 둔다
                }
            }
        }
    }

    private suspend fun acceptPendingInvite() {
        val accept = acceptInvite ?: return
        val result = try {
            accept()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        // 참여했으면 가족 정보를 다시 받아 바로 참여자로 보이게 한다
        if (result is InviteAcceptResult.Joined) home.invalidateFamily()
        result.message?.let { _messages.trySend(it) }
    }
}
