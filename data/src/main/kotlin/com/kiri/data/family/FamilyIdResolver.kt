package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.NoFamilyException
import kotlinx.serialization.json.Json

// 내 가족 공간 ID 를 돌려준다. 기억해 둔 값이 있으면 그대로 쓰고, 없으면(초대 코드로 들어왔거나 앱을 다시 설치한 경우)
// 가족 ID 조회 API 로 받아 기억한다. 소속된 가족이 없으면 NoFamilyException 으로 실패한다
internal suspend fun FamilyIdStore.resolve(api: FamilyApi, json: Json): Result<Long> {
    get()?.let { return Result.success(it) }
    return apiResult(json) { api.getMyFamilyId() }
        .map { it.familyId }
        .onSuccess { save(it) }
        .recoverCatching { error ->
            // 명세에 가족 없음 코드 예시가 없어 FAMILY404 로 시작하는 코드를 가족 없음으로 본다
            if (error is KiriApiException && error.code.startsWith(FAMILY_NOT_FOUND_PREFIX)) throw NoFamilyException()
            throw error
        }
}

private const val FAMILY_NOT_FOUND_PREFIX = "FAMILY404"
