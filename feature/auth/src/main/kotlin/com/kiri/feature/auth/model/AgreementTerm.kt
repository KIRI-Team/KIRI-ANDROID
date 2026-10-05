package com.kiri.feature.auth.model

// TODO: 실제 약관 목록과 상세 페이지 링크가 정해지면 제목/필수 여부/url 교체 (현재 Figma 문구 그대로)
enum class AgreementTerm(
    val title: String,
    val required: Boolean,
    val url: String,
) {
    TERM_1("[필수] 서비스 이용약관", required = true, url = ""),
    TERM_2("[필수] 서비스 이용약관", required = true, url = ""),
    TERM_3("[필수] 서비스 이용약관", required = true, url = ""),
    TERM_4("[필수] 서비스 이용약관", required = true, url = ""),
}
