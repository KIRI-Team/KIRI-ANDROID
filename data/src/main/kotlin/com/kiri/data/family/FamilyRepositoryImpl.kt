package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.family.Family
import com.kiri.domain.repository.FamilyRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class FamilyRepositoryImpl @Inject constructor(
    private val api: FamilyApi,
    private val familyIdStore: FamilyIdStore,
    private val json: Json,
) : FamilyRepository {

    // 만들면 마이페이지에서 초대를 불러올 수 있도록 가족 ID를 기억한다
    override suspend fun createFamily(name: String): Result<Family> =
        apiResult(json) { api.createFamily(CreateFamilyRequest(name)) }
            .mapCatching { response ->
                familyIdStore.save(response.familyId)
                response.toFamily()
            }
            .recoverDuplicateName()

    // 가족 ID 는 기억해 둔 값을, 없으면 서버에서 받아 쓴다. 소속된 가족이 없으면 바꾸지 않고 실패한다
    override suspend fun renameFamily(name: String): Result<String> {
        val familyId = familyIdStore.resolve(api, json).getOrElse { return Result.failure(it) }
        return apiResult(json) { api.renameFamily(familyId, RenameFamilyRequest(name)) }
            .map { it.name }
            .recoverDuplicateName()
    }

    // 가족 이름 중복은 화면에서 따로 안내해야 해서 도메인 오류로 바꾼다. 나머지 실패는 그대로 돌려준다
    private fun <T> Result<T>.recoverDuplicateName(): Result<T> = recoverCatching { error ->
        if (error is KiriApiException && error.code == DUPLICATE_FAMILY_NAME_CODE) throw DuplicateFamilyNameException()
        throw error
    }

    private companion object {
        const val DUPLICATE_FAMILY_NAME_CODE = "FAMILY409_1"
    }
}
