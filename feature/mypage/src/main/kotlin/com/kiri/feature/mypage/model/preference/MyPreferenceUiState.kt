package com.kiri.feature.mypage.model.preference

// 취향은 필수가 아니라 처음에는 모든 항목이 비어 있다
data class MyPreferenceUiState(
    val nickname: String = "",
    val categories: Set<PreferenceCategory> = emptySet(),
    val space: PreferenceSpace? = null,
    val activity: PreferenceActivity? = null,
    val price: PreferencePrice? = null,
    val duration: PreferenceDuration? = null,
) {
    companion object {
        const val MAX_CATEGORIES = 3
    }
}
