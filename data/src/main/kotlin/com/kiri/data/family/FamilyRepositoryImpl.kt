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
    private val geocoder: AddressGeocoder? = null,
) : FamilyRepository {

    override suspend fun getFamilyId(): Result<Long> = apiResult(json) { api.getMyFamilyId() }
        .mapCatching { familyIdStore.save(it.familyId); it.familyId }

    override suspend fun resolveAddress(postalCode: String, address: String, regionCode: String): Result<com.kiri.domain.model.family.FamilyAddress> {
        val normalized = address.trim()
        return try {
            require(postalCode.matches(Regex("[0-9]{5}")) && regionCode.matches(Regex("[0-9]{10}")) && normalized.length in 1..300) {
                "선택한 주소 정보를 확인하지 못했어요. 주소를 다시 선택해주세요."
            }
            val resolver = geocoder ?: error("주소 위치 확인을 사용할 수 없어요.")
            val location = kotlinx.coroutines.withTimeoutOrNull(10_000) { resolver.resolve(normalized) }
                ?: error("주소 위치를 찾지 못했어요. 인터넷 연결을 확인하고 주소를 다시 선택해주세요.")
            val result = com.kiri.domain.model.family.FamilyAddress(postalCode, normalized, "", regionCode, location.latitude, location.longitude)
            result.validate()
            Result.success(result)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }

    override suspend fun saveAddress(address: com.kiri.domain.model.family.FamilyAddress): Result<Unit> {
        try { address.validate() } catch (e: IllegalArgumentException) { return Result.failure(e) }
        return getFamilyId().fold(onSuccess = { id -> com.kiri.data.network.apiCommand(json) {
            api.saveAddress(id, FamilyAddressRequest(address.postalCode, address.address, address.detailAddress, address.regionCode, address.latitude, address.longitude))
        } }, onFailure = { Result.failure(it) })
    }

    // 만들면 마이페이지에서 초대를 불러올 수 있도록 가족 ID를 기억한다
    override suspend fun createFamily(name: String): Result<Family> =
        apiResult(json) { api.createFamily(CreateFamilyRequest(name)) }
            .mapCatching { response ->
                familyIdStore.save(response.familyId)
                response.toFamily()
            }
            .recoverDuplicateName()

    // 코드는 앞뒤 공백을 지우고 대문자로 보낸다. 빈 코드는 보내지 않는다. 가입하면 가족 ID 를 기억해 다른 가족 API 에 쓴다
    override suspend fun joinFamily(inviteCode: String): Result<com.kiri.domain.model.family.JoinedFamily> {
        val code = inviteCode.trim().uppercase()
        if (code.isEmpty()) return Result.failure(com.kiri.domain.model.family.InvalidInviteCodeException())
        return apiResult(json) { api.joinByCode(JoinFamilyRequest(code)) }
            .mapCatching { response ->
                familyIdStore.save(response.familyId)
                com.kiri.domain.model.family.JoinedFamily(response.familyId, response.familyName)
            }
            .recoverCatching { error ->
                val serverCode = (error as? KiriApiException)?.code ?: throw error
                throw when {
                    serverCode == "FAMILY410_1" -> com.kiri.domain.model.family.ExpiredInviteCodeException()
                    serverCode == "FAMILY409_2" -> com.kiri.domain.model.family.AlreadyInFamilyException()
                    // 코드에 맞는 가족이 없으면 FAMILY404_1(존재하지 않는 가족). FAMILY403_1 도 코드가 그 가족의 것이
                    // 아니라는 뜻이라 같은 안내를 한다
                    serverCode in INVALID_INVITE_CODES ->
                        com.kiri.domain.model.family.InvalidInviteCodeException()
                    else -> error
                }
            }
    }

    // 가족 ID 는 기억해 둔 값을, 없으면 서버에서 받아 쓴다. 소속된 가족이 없으면 바꾸지 않고 실패한다
    // 기억해 둔 가족 ID 가 오래돼 거절되면 가족 ID 를 다시 받아 한 번 더 시도한다 (withFamilyId)
    // 가족 ID 를 다시 확인하고도 FAMILY403_1 이면 그 가족의 구성원이지만 방장이 아닌 것이다 (가족명은 방장만 바꿀 수 있다)
    override suspend fun renameFamily(name: String): Result<String> =
        familyIdStore.withFamilyId(api, json) { familyId -> apiResult(json) { api.renameFamily(familyId, RenameFamilyRequest(name)) } }
            .map { it.name }
            .recoverDuplicateName()
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == NOT_OWNER_CODE) throw com.kiri.domain.model.family.NotFamilyOwnerException()
                throw error
            }

    // 가족 이름 중복은 화면에서 따로 안내해야 해서 도메인 오류로 바꾼다. 나머지 실패는 그대로 돌려준다
    private fun <T> Result<T>.recoverDuplicateName(): Result<T> = recoverCatching { error ->
        if (error is KiriApiException && error.code == DUPLICATE_FAMILY_NAME_CODE) throw DuplicateFamilyNameException()
        throw error
    }

    private companion object {
        const val DUPLICATE_FAMILY_NAME_CODE = "FAMILY409_1"
        const val NOT_OWNER_CODE = "FAMILY403_1"
        val INVALID_INVITE_CODES = setOf("COMMON400_1", "FAMILY403_1", "FAMILY404_1")
    }
}
