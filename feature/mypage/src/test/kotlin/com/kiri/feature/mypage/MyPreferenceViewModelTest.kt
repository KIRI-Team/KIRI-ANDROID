package com.kiri.feature.mypage

import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.model.taste.TasteActivity
import com.kiri.domain.model.taste.TasteCategory
import com.kiri.domain.model.taste.TasteDuration
import com.kiri.domain.model.taste.TastePrice
import com.kiri.domain.model.taste.TasteSpace
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEffect
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEvent
import com.kiri.feature.mypage.model.preference.PreferenceActivity
import com.kiri.feature.mypage.model.preference.PreferenceCategory
import com.kiri.feature.mypage.model.preference.PreferenceDuration
import com.kiri.feature.mypage.model.preference.PreferencePrice
import com.kiri.feature.mypage.model.preference.PreferenceSpace
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPreferenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TestTasteRepository()

    // 저장된 취향을 다 불러온 뒤의 ViewModel
    private fun TestScope.loadedViewModel() = MyPreferenceViewModel(repository).also { advanceUntilIdle() }

    private fun MyPreferenceViewModel.select(vararg events: MyPreferenceUiEvent) = events.forEach(::onEvent)

    @Test
    fun 저장된_취향이_없으면_닉네임만_보여주고_모든_항목이_비어_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val state = loadedViewModel().uiState.value

        assertEquals("쥬123", state.nickname)
        assertTrue(state.categories.isEmpty())
        assertNull(state.space)
        assertNull(state.activity)
        assertNull(state.price)
        assertNull(state.duration)
        assertFalse(state.isLoading)
    }

    @Test
    fun 저장된_취향이_있으면_불러온_값을_고른_상태로_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loadResult = Result.success(
            MyTaste(
                nickname = "쥬123",
                taste = Taste(
                    categories = setOf(TasteCategory.EXHIBITION, TasteCategory.FOOD_CAFE),
                    activity = TasteActivity.LIGHT,
                    space = TasteSpace.INDOOR,
                    price = TastePrice.FREE,
                    duration = TasteDuration.WITHIN_1H,
                ),
            ),
        )

        val state = loadedViewModel().uiState.value

        assertEquals(setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.FOOD_CAFE), state.categories)
        assertEquals(PreferenceSpace.INDOOR, state.space)
        assertEquals(PreferenceActivity.LIGHT, state.activity)
        assertEquals(PreferencePrice.FREE, state.price)
        assertEquals(PreferenceDuration.WITHIN_1H, state.duration)
    }

    @Test
    fun 상관없음으로_저장된_항목은_상관없음_칩을_고른_상태로_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loadResult = Result.success(
            MyTaste(nickname = "쥬123", taste = Taste(categories = setOf(TasteCategory.POPUP), activity = TasteActivity.ACTIVE)),
        )

        val state = loadedViewModel().uiState.value

        assertEquals(PreferenceSpace.ANY, state.space)
        assertEquals(PreferencePrice.ANY, state.price)
        assertEquals(PreferenceDuration.ANY, state.duration)
    }

    @Test
    fun 불러오지_못하면_비어_있는_채로_두고_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loadResult = Result.failure(IOException())

        val viewModel = loadedViewModel()

        assertTrue(viewModel.uiState.value.categories.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(MyPreferenceUiEffect.ShowLoadError, viewModel.effect.first())
    }

    @Test
    fun 불러오는_중에는_칩을_눌러도_바뀌지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MyPreferenceViewModel(repository)

        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXHIBITION))

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun 선호_활동은_3개까지만_고를_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        listOf(PreferenceCategory.EXHIBITION, PreferenceCategory.POPUP, PreferenceCategory.PERFORMANCE, PreferenceCategory.FOOD_CAFE)
            .forEach { viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(it)) }

        assertEquals(
            setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.POPUP, PreferenceCategory.PERFORMANCE),
            viewModel.uiState.value.categories,
        )
    }

    @Test
    fun 고른_선호_활동을_다시_누르면_해제되고_다른_활동을_고를_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()
        listOf(PreferenceCategory.EXHIBITION, PreferenceCategory.POPUP, PreferenceCategory.PERFORMANCE)
            .forEach { viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(it)) }

        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.POPUP))
        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.OUTDOOR))

        assertEquals(
            setOf(PreferenceCategory.EXHIBITION, PreferenceCategory.PERFORMANCE, PreferenceCategory.OUTDOOR),
            viewModel.uiState.value.categories,
        )
    }

    @Test
    fun 공간_활동량_비용_활동시간은_하나만_고르고_다시_누르면_비운다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.select(
            MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.INDOOR),
            MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.OUTDOOR),
            MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.LIGHT),
            MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.ACTIVE),
            MyPreferenceUiEvent.PriceClicked(PreferencePrice.FREE),
            MyPreferenceUiEvent.PriceClicked(PreferencePrice.FREE),
            MyPreferenceUiEvent.DurationClicked(PreferenceDuration.WITHIN_1H),
            MyPreferenceUiEvent.DurationClicked(PreferenceDuration.ANY),
        )

        val state = viewModel.uiState.value
        assertEquals(PreferenceSpace.OUTDOOR, state.space)
        assertEquals(PreferenceActivity.ACTIVE, state.activity)
        assertNull(state.price)
        assertEquals(PreferenceDuration.ANY, state.duration)
    }

    @Test
    fun 선호_활동과_활동량을_골라야_저장할_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onEvent(MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXHIBITION))
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onEvent(MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.LIGHT))
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun 저장할_수_없으면_저장을_눌러도_보내지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onEvent(MyPreferenceUiEvent.SaveClicked)
        advanceUntilIdle()

        assertTrue(repository.savedTastes.isEmpty())
    }

    @Test
    fun 고른_취향을_저장하고_상관없음과_고르지_않은_항목은_비워서_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.select(
            MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.OUTDOOR),
            MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXPERIENCE),
            MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.MODERATE),
            MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.ANY),
            MyPreferenceUiEvent.DurationClicked(PreferenceDuration.ONE_TO_THREE_HOURS),
        )

        viewModel.onEvent(MyPreferenceUiEvent.SaveClicked)

        assertEquals(MyPreferenceUiEffect.Saved, viewModel.effect.first())
        assertEquals(
            listOf(
                Taste(
                    categories = setOf(TasteCategory.OUTDOOR, TasteCategory.EXPERIENCE),
                    activity = TasteActivity.MODERATE,
                    space = null,
                    price = null,
                    duration = TasteDuration.H1_TO_3,
                ),
            ),
            repository.savedTastes,
        )
    }

    @Test
    fun 저장하지_못하면_고른_값을_그대로_두고_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.saveError = IOException()
        val viewModel = loadedViewModel()
        viewModel.select(
            MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXHIBITION),
            MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.LIGHT),
        )

        viewModel.onEvent(MyPreferenceUiEvent.SaveClicked)

        assertEquals(MyPreferenceUiEffect.ShowSaveError, viewModel.effect.first())
        assertEquals(setOf(PreferenceCategory.EXHIBITION), viewModel.uiState.value.categories)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun 저장_중에_다시_눌러도_한_번만_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()
        viewModel.select(
            MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.EXHIBITION),
            MyPreferenceUiEvent.ActivityClicked(PreferenceActivity.LIGHT),
        )

        viewModel.onEvent(MyPreferenceUiEvent.SaveClicked)
        viewModel.onEvent(MyPreferenceUiEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repository.savedTastes.size)
    }

    @Test
    fun 뒤로가기를_누르면_마이페이지로_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onEvent(MyPreferenceUiEvent.BackClicked)

        assertEquals(MyPreferenceUiEffect.NavigateBack, viewModel.effect.first())
        assertTrue(repository.savedTastes.isEmpty())
    }
}
