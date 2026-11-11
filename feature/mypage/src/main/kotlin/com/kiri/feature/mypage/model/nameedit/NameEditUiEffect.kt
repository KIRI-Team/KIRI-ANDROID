package com.kiri.feature.mypage.model.nameedit

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface NameEditUiEffect {
    // 이전 화면으로 돌아간다
    data object NavigateBack : NameEditUiEffect

    // 저장하지 못했음을 알린다 (이미 있는 이름은 입력창 아래에 따로 안내한다)
    data object ShowSaveError : NameEditUiEffect

    // 저장하지 못한 이유를 정해진 문구로 알린다 (예: 방장만 가족명을 바꿀 수 있음)
    data class ShowMessage(val message: String) : NameEditUiEffect
}
