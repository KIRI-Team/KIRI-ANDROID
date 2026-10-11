package com.kiri.feature.mypage.model.suggestion

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface SuggestionUiEvent {
    data class ContentChanged(val content: String) : SuggestionUiEvent
    data object PhotoAddClicked : SuggestionUiEvent

    // 사진 선택기에서 고른 사진 (아무것도 고르지 않으면 빈 목록)
    data class PhotosPicked(val uris: List<String>) : SuggestionUiEvent
    data class PhotoRemoveClicked(val uri: String) : SuggestionUiEvent
    data object SendClicked : SuggestionUiEvent
    data object BackClicked : SuggestionUiEvent
}
