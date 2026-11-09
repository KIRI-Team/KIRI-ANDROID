package com.kiri.domain.model.invite

// 초대 링크 형식: https://{HOST}/invite/{token}
// 링크에는 초대 코드가 아니라 서버가 발급한 초대 토큰이 들어온다
object InviteLink {
    // AndroidManifest의 intent-filter host와 같은 값이어야 한다
    const val HOST = "kiri.app"
    private const val SCHEME = "https"
    private const val PATH_PREFIX = "/invite/"

    fun urlOf(token: String): String = "$SCHEME://$HOST$PATH_PREFIX$token"

    // 초대 링크가 아니거나 토큰이 비어 있으면 null
    fun parseToken(scheme: String?, host: String?, path: String?): String? {
        if (scheme != SCHEME || host != HOST || path == null || !path.startsWith(PATH_PREFIX)) return null
        return path.removePrefix(PATH_PREFIX).trimEnd('/').takeIf { it.isNotBlank() && '/' !in it }
    }
}
