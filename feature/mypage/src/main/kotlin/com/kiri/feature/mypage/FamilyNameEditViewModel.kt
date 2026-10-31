package com.kiri.feature.mypage

import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.nickname.NicknameValidator
import com.kiri.domain.repository.FamilyRepository
import com.kiri.feature.mypage.model.common.MypageProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 바꾼 가족명을 서버에 저장한다
// TODO: 가족 정보 저장소가 생기면 실제 가족명으로 교체
@HiltViewModel
class FamilyNameEditViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
) : NameEditViewModel(
    currentName = MypageProfile.Sample.familyName,
    maxLength = NicknameValidator.FAMILY_NAME_MAX_LENGTH,
) {
    override suspend fun save(name: String): Result<Unit> = familyRepository.renameFamily(name).map { }

    override fun isDuplicated(error: Throwable): Boolean = error is DuplicateFamilyNameException
}
