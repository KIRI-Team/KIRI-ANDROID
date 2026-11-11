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
// 매개변수에 모두 기본값을 주면 Kotlin 이 인자 없는 생성자를 하나 더 만들어 Hilt 가 생성자를 고르지 못한다.
// 그래서 기본값 없이 받는다 (테스트는 null 을 넘겨 서버 저장·가족 정보 없이 쓸 수 있다)
@HiltViewModel
class NicknameEditViewModel @Inject constructor(
    private val members: MemberRepository?,
    private val home: HomeRepository?,
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
