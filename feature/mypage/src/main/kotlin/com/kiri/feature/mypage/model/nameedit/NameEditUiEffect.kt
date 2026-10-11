package com.kiri.feature.mypage.model.nameedit

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface NameEditUiEffect {
    // 이전 화면으로 돌아간다
    data object NavigateBack : NameEditUiEffect
}
