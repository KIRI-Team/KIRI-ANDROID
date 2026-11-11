package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.NoFamilyException
import kotlinx.serialization.json.Json

// 내 가족 공간 ID 를 돌려준다. 기억해 둔 값이 있으면 그대로 쓰고, 없으면(초대 코드로 들어왔거나 앱을 다시 설치한 경우)
// 가족 ID 조회 API 로 받아 기억한다. 소속된 가족이 없으면 NoFamilyException 으로 실패한다
internal suspend fun FamilyIdStore.resolve(api: FamilyApi, json: Json): Result<Long> {
    get()?.let { return Result.success(it) }
    return fetchMyFamilyId(api, json)
}

// 가족 ID 가 필요한 API(call)를 부른다. 기억해 둔 가족 ID 가 오래돼(다른 가족으로 옮긴 경우 등) 서버가
// 그 가족의 구성원이 아니거나(FAMILY403_1) 없는 가족(FAMILY404_1)이라고 거절하면, 가족 ID 를 서버에서 다시 받아
// 기억하고 바뀐 ID 로 한 번만 다시 부른다. 방금 서버에서 받은 ID 였거나 다시 받아도 같은 ID 면 다시 부르지 않는다
internal suspend fun <T> FamilyIdStore.withFamilyId(
    api: FamilyApi,
    json: Json,
    call: suspend (familyId: Long) -> Result<T>,
): Result<T> {
    val remembered = get()
    val familyId = resolve(api, json).getOrElse { return Result.failure(it) }
    val result = call(familyId)
    if (remembered == null || !result.isStaleFamily()) return result
    val fresh = fetchMyFamilyId(api, json).getOrElse { return Result.failure(it) }
    if (fresh == familyId) return result
    return call(fresh)
}

// 가족 ID 조회 API 로 내 가족 ID 를 받아 기억한다. 소속된 가족이 없으면 NoFamilyException
private suspend fun FamilyIdStore.fetchMyFamilyId(api: FamilyApi, json: Json): Result<Long> =
    apiResult(json) { api.getMyFamilyId() }
        .map { it.familyId }
        .onSuccess { save(it) }
        .recoverCatching { error ->
            // 명세에 가족 없음 코드 예시가 없어 FAMILY404 로 시작하는 코드를 가족 없음으로 본다
            if (error is KiriApiException && error.code.startsWith(FAMILY_NOT_FOUND_PREFIX)) throw NoFamilyException()
            throw error
        }

// 기억해 둔 가족 ID 가 더는 내 가족이 아니라는 거절인지
private fun Result<*>.isStaleFamily(): Boolean =
    (exceptionOrNull() as? KiriApiException)?.code in STALE_FAMILY_CODES

private const val FAMILY_NOT_FOUND_PREFIX = "FAMILY404"
private val STALE_FAMILY_CODES = setOf("FAMILY403_1", "FAMILY404_1")
