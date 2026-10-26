package com.kiri.domain.model.term

// 서버가 내려주는 약관. 회원가입 약관 동의 화면에서 쓴다
data class Term(
    val id: Long,
    val title: String,
    val content: String,
    val required: Boolean,
) {
    // 동의 화면에 쓰는 제목. 예: [필수] 서비스 이용약관
    val agreementTitle: String
        get() = "${if (required) "[필수]" else "[선택]"} $title"
}
