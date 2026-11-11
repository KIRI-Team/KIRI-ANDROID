package com.kiri.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.FamilyAccess
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.access
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 메인 화면에서 지금 사용자의 가족 이용 상태(방장·참여자·둘러보기)를 들고 있다. 하단 탭에서 둘러보기 사용자를 막는 데 쓴다
// 가족 정보(HomeRepository.family)는 잠깐 재사용되므로 화면에 돌아올 때마다 불러도 요청이 쌓이지 않는다
@HiltViewModel
class FamilyAccessViewModel @Inject constructor(
    private val home: HomeRepository,
) : ViewModel() {
    // 아직 모르면 null. 모를 때는 막지 않는다 (네트워크 실패로 가족 기능을 잘못 막지 않도록)
    private val _access = MutableStateFlow<FamilyAccess?>(null)
    val access: StateFlow<FamilyAccess?> = _access.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
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
