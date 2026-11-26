package com.kiri.feature.mypage.model.nameedit

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface NameEditUiEvent {
    data class NameChanged(val name: String) : NameEditUiEvent
    data object NextClicked : NameEditUiEvent
    data object BackClicked : NameEditUiEvent
}
