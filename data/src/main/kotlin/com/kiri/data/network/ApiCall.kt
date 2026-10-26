package com.kiri.data.network

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

// API 를 호출하고 성공 응답의 result 만 꺼낸다
// 서버가 실패로 답하면(HTTP 오류 포함) KiriApiException 으로, 연결 문제는 IOException 그대로 실패를 돌려준다
internal suspend fun <T : Any> apiResult(json: Json, call: suspend () -> ApiResponse<T>): Result<T> =
    apiCall(json, call) { it.result }

// 저장처럼 result 없이(null) 성공 여부만 알려 주는 API 용. 실패 처리는 apiResult 와 같다
internal suspend fun apiUnitResult(json: Json, call: suspend () -> ApiResponse<Unit>): Result<Unit> =
    apiCall(json, call) { }

private suspend fun <T, R : Any> apiCall(
    json: Json,
    call: suspend () -> ApiResponse<T>,
    extract: (ApiResponse<T>) -> R?,
): Result<R> = try {
    val response = call()
    val result = extract(response)
    if (response.isSuccess && result != null) {
        Result.success(result)
    } else {
        Result.failure(KiriApiException(response.code, response.message))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Result.failure(e.toKiriApiException(json))
} catch (e: Exception) {
    Result.failure(e)
}

private fun HttpException.toKiriApiException(json: Json): KiriApiException {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val error = body?.let { runCatching { json.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
    return KiriApiException(
        code = error?.code ?: "HTTP${code()}",
        message = error?.message ?: message(),
        cause = this,
    )
}
