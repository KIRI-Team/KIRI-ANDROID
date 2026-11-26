package com.kiri.feature.mypage.model.suggestion

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface SuggestionUiEffect {
    // 시스템 사진 선택기를 연다. 최대 maxCount 장까지 고를 수 있다
    data class OpenPhotoPicker(val maxCount: Int) : SuggestionUiEffect

    // 토스트로 알린다 (보내기 성공·실패)
    data class ShowMessage(val message: String) : SuggestionUiEffect

    // 마이페이지로 돌아간다
    data object NavigateBack : SuggestionUiEffect
}
