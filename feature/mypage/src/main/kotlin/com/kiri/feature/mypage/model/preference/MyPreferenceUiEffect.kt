package com.kiri.feature.mypage.model.preference

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface MyPreferenceUiEffect {
    // 마이페이지로 돌아간다
    data object NavigateBack : MyPreferenceUiEffect

    // 저장했음을 알리고 마이페이지로 돌아간다
    data object Saved : MyPreferenceUiEffect

    // 저장된 취향을 불러오지 못했다 (비어 있는 채로 새로 고를 수 있다)
    data object ShowLoadError : MyPreferenceUiEffect

    // 저장하지 못했다 (고른 값은 그대로 둔다)
    data object ShowSaveError : MyPreferenceUiEffect
}
