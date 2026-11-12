package com.kiri.feature.mypage

import com.kiri.domain.repository.ReviewPhotoRepository
import com.kiri.domain.repository.SuggestionRepository
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEffect
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_CONTENT_LENGTH
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_PHOTO_COUNT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
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

    // 보낸 건의를 기억한다. result 를 바꾸면 실패를, gate 를 주면 끝나기 전 상태를 볼 수 있다
    private class Suggestions : SuggestionRepository {
        val sent = mutableListOf<Pair<String, String?>>()
        var result: Result<Long> = Result.success(1L)
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun send(content: String, imageKey: String?): Result<Long> {
            sent += content to imageKey
            gate?.await()
            return result
        }
    }

    private class Photos : ReviewPhotoRepository {
        val uploads = mutableListOf<String>()
        var fail = false
        override suspend fun upload(uri: String): Result<String> {
            uploads += uri
            return if (fail) Result.failure(Exception()) else Result.success("key-$uri")
        }
    }

    private val suggestions = Suggestions()
    private val photos = Photos()
    private val viewModel = SuggestionViewModel(suggestions, photos)

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
    fun 사진은_API에_맞춰_한_장만_고를_수_있다() {
        assertEquals(1, MAX_PHOTO_COUNT)
    }

    @Test
    fun 사진_추가를_누르면_남은_장수로_사진_선택기를_연다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.PhotoAddClicked)

        assertEquals(SuggestionUiEffect.OpenPhotoPicker(MAX_PHOTO_COUNT), viewModel.effect.first())
    }

    @Test
    fun 이미_고른_사진은_다시_넣지_않는다() {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("a", "a")))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("a")))

        assertEquals(listOf("a"), viewModel.uiState.value.photoUris)
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
        viewModel.onEvent(SuggestionUiEvent.PhotoRemoveClicked("content://photo/0"))

        assertTrue(viewModel.uiState.value.photoUris.isEmpty())
        assertTrue(viewModel.uiState.value.canAddPhoto)
    }

    @Test
    fun 선택기에서_아무것도_고르지_않으면_그대로다() {
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("a")))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(emptyList()))

        assertEquals(listOf("a"), viewModel.uiState.value.photoUris)
    }

    @Test
    fun 사진_없이_보내면_내용만_보내고_감사_안내_뒤_돌아간다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("  알림이 늦게 와요 "))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertEquals(listOf("알림이 늦게 와요" to null), suggestions.sent)
        assertTrue(photos.uploads.isEmpty())
        assertEquals(
            listOf(SuggestionUiEffect.ShowMessage(SuggestionViewModel.SENT_MESSAGE), SuggestionUiEffect.NavigateBack),
            viewModel.effect.take(2).toList(),
        )
        assertFalse(viewModel.uiState.value.sending)
    }

    @Test
    fun 사진이_있으면_먼저_올리고_받은_이미지_키를_함께_보낸다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("화면이 깨져요"))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("content://photo/0")))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertEquals(listOf("content://photo/0"), photos.uploads)
        assertEquals(listOf("화면이 깨져요" to "key-content://photo/0"), suggestions.sent)
    }

    @Test
    fun 사진을_올리지_못하면_보내지_않고_안내하며_내용을_그대로_둔다() = runTest {
        photos.fail = true
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("화면이 깨져요"))
        viewModel.onEvent(SuggestionUiEvent.PhotosPicked(listOf("content://photo/0")))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertTrue(suggestions.sent.isEmpty())
        assertEquals(SuggestionUiEffect.ShowMessage(SuggestionViewModel.PHOTO_FAILED_MESSAGE), viewModel.effect.first())
        assertEquals("화면이 깨져요", viewModel.uiState.value.content)
        assertEquals(listOf("content://photo/0"), viewModel.uiState.value.photoUris)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun 서버가_거절하면_돌아가지_않고_안내한_뒤_다시_보낼_수_있다() = runTest {
        suggestions.result = Result.failure(Exception("COMMON500_1"))
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("좋아요"))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertEquals(SuggestionUiEffect.ShowMessage(SuggestionViewModel.SEND_FAILED_MESSAGE), viewModel.effect.first())
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun 보내는_중에는_다시_눌러도_한_번만_보낸다() = runTest {
        suggestions.gate = CompletableDeferred()
        viewModel.onEvent(SuggestionUiEvent.ContentChanged("좋아요"))
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sending)
        assertFalse(viewModel.uiState.value.canSend)
        viewModel.onEvent(SuggestionUiEvent.SendClicked)

        suggestions.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, suggestions.sent.size)
    }

    @Test
    fun 내용이_비어_있으면_의견_보내기를_눌러도_보내지_않는다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.SendClicked)
        advanceUntilIdle()

        assertTrue(suggestions.sent.isEmpty())
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(SuggestionUiEvent.BackClicked)

        assertEquals(SuggestionUiEffect.NavigateBack, viewModel.effect.first())
    }
}
