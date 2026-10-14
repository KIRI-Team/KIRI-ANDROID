package com.kiri.data.network

// 서버가 실패로 답했을 때. code 는 서버 응답의 code (예: AUTH401_3), HTTP 오류만 있으면 HTTP{상태코드}
class KiriApiException(
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
