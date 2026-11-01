package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEffect
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEvent
import com.kiri.feature.mypage.model.preference.PreferenceActivity
import com.kiri.feature.mypage.model.preference.PreferenceCategory
import com.kiri.feature.mypage.model.preference.PreferenceDuration
import com.kiri.feature.mypage.model.preference.PreferencePrice
import com.kiri.feature.mypage.model.preference.PreferenceSpace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPreferenceViewModelTest {

    private val viewModel = MyPreferenceViewModel()
    private val state get() = viewModel.uiState.value

    @Test
    fun 처음에는_모든_항목이_비어_있고_닉네임을_보여준다() {
        assertEquals(MypageProfile.Sample.nickname, state.nickname)
        assertTrue(state.categories.isEmpty())
        assertNull(state.space)
        assertNull(state.activity)
        assertNull(state.price)
        assertNull(state.duration)
    }

    @Test
    fun 선호_활동은_여러_개를_고를_수_있다() {
        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXHIBITION))
        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.FOOD_CAFE))

        assertEquals(setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.FOOD_CAFE), state.categories)
    }

    @Test
    fun 선호_활동을_3개_고르면_더_고를_수_없다() {
        listOf(
            PreferenceCategory.EXHIBITION,
            PreferenceCategory.POPUP,
            PreferenceCategory.PERFORMANCE,
            PreferenceCategory.FOOD_CAFE,
        ).forEach { viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(it)) }

        assertEquals(
            setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.POPUP, PreferenceCategory.PERFORMANCE),
            state.categories,
        )
    }

    @Test
    fun 고른_선호_활동을_다시_누르면_해제되고_다른_활동을_고를_수_있다() {
        listOf(PreferenceCategory.EXHIBITION, PreferenceCategory.POPUP, PreferenceCategory.PERFORMANCE)
            .forEach { viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(it)) }

        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.POPUP))
        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.FOOD_CAFE))

        assertEquals(
            setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.PERFORMANCE, PreferenceCategory.FOOD_CAFE),
            state.categories,
        )
    }

    @Test
    fun 공간_활동량_비용_활동시간은_하나만_고른다() {
        viewModel.onEvent(MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.INDOOR))
        viewModel.onEvent(MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.OUTDOOR))
        viewModel.onEvent(MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.LIGHT))
        viewModel.onEvent(MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.ACTIVE))
        viewModel.onEvent(MyPreferenceUiEvent.PriceClicked(PreferencePrice.FREE))
        viewModel.onEvent(MyPreferenceUiEvent.PriceClicked(PreferencePrice.ANY))
        viewModel.onEvent(MyPreferenceUiEvent.DurationClicked(PreferenceDuration.WITHIN_1H))
        viewModel.onEvent(MyPreferenceUiEvent.DurationClicked(PreferenceDuration.ANY))

        assertEquals(PreferenceSpace.OUTDOOR, state.space)
        assertEquals(PreferenceActivity.ACTIVE, state.activity)
        assertEquals(PreferencePrice.ANY, state.price)
        assertEquals(PreferenceDuration.ANY, state.duration)
    }

    @Test
    fun 고른_항목을_다시_누르면_비운다() {
        viewModel.onEvent(MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.INDOOR))
        viewModel.onEvent(MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.INDOOR))
        viewModel.onEvent(MyPreferenceUiEvent.DurationClicked(PreferenceDuration.WITHIN_1H))
        viewModel.onEvent(MyPreferenceUiEvent.DurationClicked(PreferenceDuration.WITHIN_1H))

        assertNull(state.space)
        assertNull(state.duration)
    }

    @Test
    fun 뒤로가기를_누르면_마이페이지로_돌아간다() = runTest {
        viewModel.onEvent(MyPreferenceUiEvent.BackClicked)

        assertEquals(MyPreferenceUiEffect.NavigateBack, viewModel.effect.first())
    }
}
