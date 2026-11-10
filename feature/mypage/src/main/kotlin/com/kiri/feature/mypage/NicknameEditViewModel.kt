package com.kiri.feature.mypage

import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.model.nickname.DuplicateNicknameException
import com.kiri.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 바꾼 닉네임을 서버에 저장한다 (PATCH api/v1/members/nickname). 지금 닉네임은 가족 정보 API 의 내 구성원 정보로 불러온다
@HiltViewModel
class NicknameEditViewModel @Inject constructor(
    private val members: MemberRepository? = null,
    private val home: HomeRepository? = null,
) : NameEditViewModel("") {
    init {
        home?.let { source ->
            viewModelScope.launch {
                try {
                    val family = source.family()
                    family.members.firstOrNull { it.id == family.currentUserId }?.nickname?.let(::updateCurrentName)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { }
            }
        }
    }

    // 저장하면 가족 정보 캐시를 비워, 돌아간 화면(마이페이지·가족 기본 설정)이 바뀐 닉네임을 바로 보여주게 한다
    override suspend fun save(name: String): Result<Unit> {
        val repository = members ?: return Result.success(Unit)
        return repository.updateNickname(name).onSuccess { home?.invalidateFamily() }
    }

    override fun isDuplicated(error: Throwable): Boolean = error is DuplicateNicknameException
}
