package com.kiri.feature.mypage.model.preference

import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste

// 취향은 필수가 아니라 저장된 취향이 없으면 모든 항목이 비어 있다
data class MyPreferenceUiState(
    val nickname: String = "",
    val categories: Set<PreferenceCategory> = emptySet(),
    val space: PreferenceSpace? = null,
    val activity: PreferenceActivity? = null,
    val price: PreferencePrice? = null,
    val duration: PreferenceDuration? = null,
    // 저장된 취향을 불러오는 중
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    // 서버 규칙: 선호 활동 1~3개, 활동량 필수. 나머지는 고르지 않으면 상관없음으로 저장한다
    val canSave: Boolean
        get() = !isLoading && !isSaving && categories.size in 1..Taste.MAX_CATEGORIES && activity != null

    fun toTaste(): Taste = Taste(
        categories = categories.map { it.taste }.toSet(),
        activity = requireNotNull(activity).taste,
        space = space?.taste,
        price = price?.taste,
        duration = duration?.taste,
    )

    // 저장된 취향을 화면 선택으로 바꾼다. 취향을 정한 적이 없으면 모두 비우고,
    // 정했다면 null(상관없음)인 항목은 상관없음 칩을 고른 것으로 보여준다
    fun withTaste(myTaste: MyTaste): MyPreferenceUiState {
        val taste = myTaste.taste ?: return copy(
            nickname = myTaste.nickname,
            categories = emptySet(),
            space = null,
            activity = null,
            price = null,
            duration = null,
        )
        return copy(
            nickname = myTaste.nickname,
            categories = PreferenceCategory.entries.filter { it.taste in taste.categories }.toSet(),
            space = PreferenceSpace.entries.first { it.taste == taste.space },
            activity = PreferenceActivity.entries.first { it.taste == taste.activity },
            price = PreferencePrice.entries.first { it.taste == taste.price },
            duration = PreferenceDuration.entries.first { it.taste == taste.duration },
        )
    }
}
