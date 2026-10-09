package com.kiri.domain.model.invite

// 초대 링크 형식: https://{HOST}/invite/{code}
object InviteLink {
    // TODO(invite-domain): 실제 도메인이 정해지면 교체 (AndroidManifest의 intent-filter host도 함께 수정)
    const val HOST = "kiri.example.com"
    private const val SCHEME = "https"
    private const val PATH_PREFIX = "/invite/"

    fun urlOf(code: String): String = "$SCHEME://$HOST$PATH_PREFIX$code"

    // 초대 링크가 아니거나 코드가 비어 있으면 null
    fun parseCode(scheme: String?, host: String?, path: String?): String? {
        if (scheme != SCHEME || host != HOST || path == null || !path.startsWith(PATH_PREFIX)) return null
        return path.removePrefix(PATH_PREFIX).trimEnd('/').takeIf { it.isNotEmpty() && '/' !in it }
    }
}
