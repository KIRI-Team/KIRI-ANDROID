package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.FamilyRepository
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class FamilyRepositoryImpl @Inject constructor(
    private val api: FamilyApi,
    private val json: Json,
) : FamilyRepository {

    // 가족 이름 중복은 화면에서 따로 안내해야 해서 도메인 오류로 바꾼다. 나머지 실패는 그대로 돌려준다
    override suspend fun createFamily(name: String): Result<Family> =
        apiResult(json) { api.createFamily(CreateFamilyRequest(name)) }
            .map { it.toFamily() }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == DUPLICATE_FAMILY_NAME_CODE) throw DuplicateFamilyNameException()
                throw error
            }

    private fun CreateFamilyResponse.toFamily() = Family(
        id = familyId,
        name = name,
        invite = Invite(
            code = inviteCode,
            url = inviteUrl,
            // 만료일을 읽지 못해도 초대는 쓸 수 있으니 만료일만 비운다
            expiresAt = inviteExpiresAt?.let { runCatching { LocalDateTime.parse(it).toLocalDate() }.getOrNull() },
        ),
    )

    private companion object {
        const val DUPLICATE_FAMILY_NAME_CODE = "FAMILY409_1"
    }
}
