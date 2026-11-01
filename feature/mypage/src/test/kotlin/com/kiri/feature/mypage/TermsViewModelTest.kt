package com.kiri.feature.mypage

import com.kiri.domain.model.term.Term
import com.kiri.domain.repository.TermRepository
import com.kiri.feature.mypage.model.terms.TermsUiEffect
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import java.io.IOException
import java.time.LocalDate
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
class TermsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val serviceTerm = Term(
        id = 1,
        title = "서비스 이용약관",
        content = "제1조(목적) ...",
        required = true,
        updatedAt = LocalDate.of(2026, 10, 4),
    )
    private val privacyTerm = Term(
        id = 2,
        title = "개인정보 수집 및 이용 동의",
        content = "수집 항목 ...",
        required = true,
        updatedAt = LocalDate.of(2026, 10, 20),
    )

    // 약관 목록 결과를 정해두고 요청 횟수를 센다
    private class TestTermRepository(var result: Result<List<Term>>) : TermRepository {
        var requestCount = 0
            private set

        override suspend fun getTerms(): Result<List<Term>> {
            requestCount++
            return result
        }

        override suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit> = Result.success(Unit)
    }

    private val repository = TestTermRepository(Result.success(listOf(serviceTerm, privacyTerm)))

    private fun TestScope.loadedViewModel() = TermsViewModel(repository).also { advanceUntilIdle() }

    @Test
    fun 서버에서_받은_약관_목록을_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        val state = loadedViewModel().uiState.value

        assertEquals(listOf(serviceTerm, privacyTerm), state.terms)
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertNull(state.viewingTerm)
    }

    @Test
    fun 최종_업데이트일은_가장_최근에_바뀐_약관의_날짜다() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals("최종 업데이트 | 26. 10. 20", loadedViewModel().uiState.value.lastUpdatedText)
    }

    @Test
    fun 업데이트일을_모르면_최종_업데이트일을_표시하지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.result = Result.success(listOf(serviceTerm.copy(updatedAt = null)))

        assertNull(loadedViewModel().uiState.value.lastUpdatedText)
    }

    @Test
    fun 불러오는_동안에는_로딩_중이다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = TermsViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.terms.isEmpty())
    }

    @Test
    fun 불러오지_못하면_실패를_표시하고_다시_시도할_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        repository.result = Result.failure(IOException())
        val viewModel = loadedViewModel()
        assertTrue(viewModel.uiState.value.loadFailed)

        repository.result = Result.success(listOf(serviceTerm))
        viewModel.onEvent(TermsUiEvent.RetryClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf(serviceTerm), viewModel.uiState.value.terms)
        assertEquals(2, repository.requestCount)
    }

    @Test
    fun 불러오는_중에_다시_시도를_눌러도_한_번만_요청한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = TermsViewModel(repository)

        viewModel.onEvent(TermsUiEvent.RetryClicked)
        advanceUntilIdle()

        assertEquals(1, repository.requestCount)
    }

    @Test
    fun 약관을_누르면_세부_내용을_열고_닫으면_비운다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onEvent(TermsUiEvent.TermClicked(privacyTerm))
        assertEquals(privacyTerm, viewModel.uiState.value.viewingTerm)

        viewModel.onEvent(TermsUiEvent.TermDetailClosed)
        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = loadedViewModel()

        viewModel.onEvent(TermsUiEvent.BackClicked)

        assertEquals(TermsUiEffect.NavigateBack, viewModel.effect.first())
    }
}
