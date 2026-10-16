package com.kiri.data.network

import kotlinx.serialization.Serializable

// 서버의 모든 응답을 감싸는 공통 형식
// { "isSuccess": true, "code": "AUTH200_1", "message": "로그인 성공", "result": { ... } }
@Serializable
internal data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T? = null,
)

// 실패 응답에서는 result 모양이 제각각이라 code, message 만 읽는다
@Serializable
internal data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
)
