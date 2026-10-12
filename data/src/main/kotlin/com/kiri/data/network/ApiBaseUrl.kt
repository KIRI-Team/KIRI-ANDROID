package com.kiri.data.network

// Retrofit 은 base URL 이 / 로 끝나야 해서 맞춰 준다. 설정하지 않았으면 어디서 넣는지 알려 주고 멈춘다
internal fun apiBaseUrl(configured: String): String {
    val url = configured.trim()
    check(url.isNotEmpty()) { "KIRI_API_BASE_URL 이 설정되지 않았습니다. local.properties 를 확인하세요." }
    require(url.startsWith("https://") || url.startsWith("http://")) { "KIRI_API_BASE_URL 은 http(s):// 로 시작해야 합니다: $url" }
    return if (url.endsWith("/")) url else "$url/"
}
