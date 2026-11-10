package com.kiri.data.auth

import com.kiri.data.network.kiriJson
import java.util.Base64
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// 로그인 토큰(JWT)의 sub 에 담긴 내 회원 id 를 꺼낸다. 형식이 다르거나 읽지 못하면 null.
// 내 회원 정보를 주는 API 가 없어 가족 구성원 중 "나"를 찾는 데 쓴다. 서명은 확인하지 않는다(서버가 확인한다)
internal fun memberIdOf(accessToken: String): String? = runCatching {
    val payload = accessToken.split('.').getOrNull(1) ?: return null
    val decoded = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
    kiriJson.parseToJsonElement(decoded).jsonObject["sub"]?.jsonPrimitive?.content
}.getOrNull()?.takeIf { it.isNotBlank() }
