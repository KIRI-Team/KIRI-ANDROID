package com.kiri.feature.mypage.model.preference

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface MyPreferenceUiEvent {
    data class CategoryClicked(val category: PreferenceCategory) : MyPreferenceUiEvent
    data class SpaceClicked(val space: PreferenceSpace) : MyPreferenceUiEvent
    data class ActivityClicked(val activity: PreferenceActivity) : MyPreferenceUiEvent
    data class PriceClicked(val price: PreferencePrice) : MyPreferenceUiEvent
    data class DurationClicked(val duration: PreferenceDuration) : MyPreferenceUiEvent
    data object SaveClicked : MyPreferenceUiEvent
    data object BackClicked : MyPreferenceUiEvent
}
