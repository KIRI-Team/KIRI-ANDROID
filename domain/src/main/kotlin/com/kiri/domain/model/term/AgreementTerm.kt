package com.kiri.domain.model.term

import java.time.LocalDate

// 마이페이지 이용 약관 화면이 쓰는 임시 약관 목록 (회원가입 약관 동의는 서버 약관 Term 을 쓴다)
// TODO: 마이페이지를 약관 목록 API(TermRepository.getTerms)에 연결하면 Term 으로 바꾸고 이 enum 을 지운다
enum class AgreementTerm(
    val displayName: String,
    val required: Boolean,
    val content: String,
) {
    TERM_1("서비스 이용약관", required = true, content = EXAMPLE_CONTENT),
    TERM_2("서비스 이용약관", required = true, content = EXAMPLE_CONTENT),
    TERM_3("서비스 이용약관", required = true, content = EXAMPLE_CONTENT),
    TERM_4("서비스 이용약관", required = true, content = EXAMPLE_CONTENT),
    ;

    // 동의 화면에 쓰는 제목. 예: [필수] 서비스 이용약관
    val title: String
        get() = "${if (required) "[필수]" else "[선택]"} $displayName"

    companion object {
        val LAST_UPDATED: LocalDate = LocalDate.of(2026, 6, 26)
    }
}

private const val EXAMPLE_CONTENT = """제1조 (목적)
본 약관은 KIRI(이하 "서비스")의 이용과 관련하여 서비스와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.

제2조 (정의)
1. "이용자"란 본 약관에 따라 서비스를 이용하는 회원을 말합니다.
2. "가족 공간"이란 이용자가 가족 구성원과 함께 기록을 공유하는 공간을 말합니다.

제3조 (약관의 효력 및 변경)
1. 본 약관은 서비스 화면에 게시하여 이용자에게 공지함으로써 효력이 발생합니다.
2. 서비스는 관련 법령을 위배하지 않는 범위에서 본 약관을 변경할 수 있습니다.

※ 위 내용은 예시 문구이며 실제 약관으로 교체될 예정입니다."""
