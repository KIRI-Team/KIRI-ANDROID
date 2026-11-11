package com.kiri.feature.mypage

import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.nickname.NicknameValidator
import com.kiri.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 바꾼 가족명을 서버에 저장한다. 지금 가족명은 가족 정보 API 로 불러온다
@HiltViewModel
class FamilyNameEditViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val home: HomeRepository? = null,
) : NameEditViewModel(
    currentName = "",
    maxLength = NicknameValidator.FAMILY_NAME_MAX_LENGTH,
) {
    init {
        home?.let { source ->
            viewModelScope.launch {
                try { source.family().familyName?.let(::updateCurrentName) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { }
            }
        }
    }

    // 저장하면 가족 정보 캐시를 비워, 돌아간 화면이 바뀐 이름을 바로 보여주게 한다
    override suspend fun save(name: String): Result<Unit> =
        familyRepository.renameFamily(name).map { }.onSuccess { home?.invalidateFamily() }

    override fun isDuplicated(error: Throwable): Boolean = error is DuplicateFamilyNameException

    override fun failureMessage(error: Throwable): String? =
        if (error is com.kiri.domain.model.family.NotFamilyOwnerException) OWNER_ONLY_MESSAGE else null

    companion object {
        const val OWNER_ONLY_MESSAGE = "방장만 가족명을 바꿀 수 있어요."
    }
}
