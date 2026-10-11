package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.suggestion.SuggestionUiEffect
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_CONTENT_LENGTH
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_PHOTO_COUNT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SuggestionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = SuggestionViewModel()

    private fun photos(count: Int, prefix: String = "content://photo/") = List(count) { "$prefix$it" }

    @Test
    fun 처음에는_내용이_비어_있어_보낼_수_없다() {
        val state = viewModel.uiState.value

        assertEquals("0/500", state.contentCountText)
        assertFalse(state.canSend)
        assertTrue(state.canAddPhoto)
    }

    @Test
    fun 내용을_적으면_글자_수가_바뀌고_보낼_수_있다() {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("좋아요"))

        assertEquals("3/500", viewModel.uiState.value.contentCountText)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun 공백만_적으면_보낼_수_없다() {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("   "))

        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun 내용은_500자를_넘으면_잘라낸다() {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("가".repeat(MAX_CONTENT_LENGTH + 20)))

        assertEquals(MAX_CONTENT_LENGTH, viewModel.uiState.value.content.length)
        assertEquals("500/500", viewModel.uiState.value.contentCountText)
    }

    @Test
    fun 사진_추가를_누르면_남은_장수로_사진_선택기를_연다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(photos(2)))
        viewModel.onEvent(SuggestionUiEvent.PhotoAddClicked)

        assertEquals(SuggestionUiEffect.OpenPhotoPicker(MAX_PHOTO_COUNT - 2), viewModel.effect.first())
    }

    @Test
    fun 고른_사진은_순서대로_붙고_이미_고른_사진은_다시_넣지_않는다() {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("a", "b")))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("b", "c", "c")))

        assertEquals(listOf("a", "b", "c"), viewModel.uiState.value.photoUris)
    }

    @Test
    fun 사진은_최대_장수까지만_붙고_가득_차면_선택기를_열지_않는다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(photos(MAX_PHOTO_COUNT + 2)))

        assertEquals(photos(MAX_PHOTO_COUNT), viewModel.uiState.value.photoUris)
        assertFalse(viewModel.uiState.value.canAddPhoto)

        viewModel.onEvent(SuggestionUiEvent.PhotoAddClicked)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 사진을_빼면_다시_추가할_수_있다() {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(photos(MAX_PHOTO_COUNT)))
        viewModel.onEvent(SuggestionUiEvent.PhotoRemoveClicked("content://photo/1"))

        assertEquals(MAX_PHOTO_COUNT - 1, viewModel.uiState.value.photoUris.size)
        assertFalse("content://photo/1" in viewModel.uiState.value.photoUris)
        assertTrue(viewModel.uiState.value.canAddPhoto)
    }

    @Test
    fun 선택기에서_아무것도_고르지_않으면_그대로다() {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("a")))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(emptyList()))

        assertEquals(listOf("a"), viewModel.uiState.value.photoUris)
    }

    @Test
    fun 내용이_있을_때_의견_보내기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("알림이 늦게 와요"))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)

        assertEquals(SuggestionUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 내용이_비어_있으면_의견_보내기를_눌러도_돌아가지_않는다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.SendClicked)

        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.BackClicked)

        assertEquals(SuggestionUiEffect.NavigateBack, viewModel.effect.first())
    }
}
