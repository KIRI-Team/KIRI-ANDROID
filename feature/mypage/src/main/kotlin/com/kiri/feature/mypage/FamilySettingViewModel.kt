package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeRepository
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.family.FamilySettingUiState
import com.kiri.feature.mypage.model.family.toFamilySettingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 마이페이지 프로필과 가족 기본 설정이 함께 쓰는 가족 정보 (가족 이름, 구성원, 나)
// 화면에 다시 돌아올 때마다(가족명·닉네임을 바꾸고 온 경우 등) load() 로 새로 불러온다
@HiltViewModel
class FamilySettingViewModel @Inject constructor(
    private val home: HomeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FamilySettingUiState())
    val uiState: StateFlow<FamilySettingUiState> = _uiState.asStateFlow()

    private var loading = false

    fun load() {
        if (loading) return
        loading = true
        // 이미 보여주고 있는 값은 그대로 두고 새 값으로 바꾼다
        _uiState.update { it.copy(loading = it.members.isEmpty() && it.familyName == null, failed = false) }
        viewModelScope.launch {
            try {
                val family = home.family()
                _uiState.value = family.toFamilySettingUiState()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, failed = true) }
            } finally {
                loading = false
            }
        }
    }
}

// 마이페이지 상단 프로필 (닉네임, 가족 이름 I 역할). 불러오기 전이나 가족이 없으면 비어 있다
fun FamilySettingUiState.toProfile(): MypageProfile = MypageProfile(
    nickname = me?.nickname.orEmpty(),
    familyName = familyName.orEmpty(),
    role = me?.role.orEmpty(),
)
