package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.repository.MemberRepository
import com.kiri.feature.mypage.model.family.label
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
// 가족이 없는 둘러보기 회원은 가족 정보에 내가 없으므로, 프로필은 내 정보 조회로 채운다
@HiltViewModel
class FamilySettingViewModel @Inject constructor(
    private val home: HomeRepository,
    private val members: MemberRepository? = null,
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
            // 내 정보는 프로필 보조용이라 불러오지 못해도 가족 정보는 그대로 보여준다. 실패하면 알고 있던 값을 둔다
            val myInfo = members?.me()?.getOrNull() ?: _uiState.value.myInfo
            try {
                val family = home.family()
                _uiState.value = family.toFamilySettingUiState().copy(myInfo = myInfo)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, failed = true, myInfo = myInfo) }
            } finally {
                loading = false
            }
        }
    }
}

// 마이페이지 상단 프로필 (닉네임, 가족 이름 I 역할). 가족 구성원 중 내 정보를 먼저 쓰고,
// 가족이 없으면(둘러보기) 내 정보 조회 값을 쓴다. 둘 다 없으면 비어 있다
fun FamilySettingUiState.toProfile(): MypageProfile = MypageProfile(
    nickname = me?.nickname ?: myInfo?.nickname.orEmpty(),
    familyName = familyName ?: myInfo?.familyName.orEmpty(),
    role = me?.role ?: myInfo?.role?.label.orEmpty(),
)
