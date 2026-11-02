package com.kiri.feature.mypage

import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteReissueFailedException
import com.kiri.feature.mypage.model.invite.InviteLinkUiEffect
import com.kiri.feature.mypage.model.invite.InviteLinkUiEvent
import com.kiri.feature.mypage.model.invite.InviteLinkUiState
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InviteLinkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val issued = Invite(
        code = "NWTKYJ",
        url = "https://kiri.example.com/invite/NWTKYJ",
        expiresAt = LocalDate.of(2026, 10, 12),
    )

    private fun viewModel(repository: TestInviteRepository) = InviteLinkViewModel(repository)

    @Test
    fun 서버에서_불러온_지금_초대를_새로_발급하지_않고_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(current = issued)
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertEquals(issued, viewModel.uiState.value.invite)
        assertFalse(viewModel.uiState.value.isIssuing)
        assertEquals(0, repository.createCount)
    }

    @Test
    fun 초대를_불러오지_못하면_새로_발급하지_않고_오류를_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(loadError = IOException())
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.invite)
        assertFalse(viewModel.uiState.value.isIssuing)
        assertEquals(0, repository.createCount)
        assertEquals(InviteLinkUiEffect.ShowLoadError, viewModel.effect.first())
    }

    @Test
    fun 초대를_불러오지_못해도_리셋으로_새로_발급할_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(loadError = IOException())
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        assertEquals(InviteLinkUiEffect.ShowLoadError, viewModel.effect.first())

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        assertEquals(TestInviteRepository.NEW_INVITE, viewModel.uiState.value.invite)
        assertEquals(1, repository.createCount)
    }

    @Test
    fun 리셋하기를_누르면_최소_1초_로딩_후_새_초대로_바뀐다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(current = issued)
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceTimeBy(999)
        runCurrent()
        assertTrue(viewModel.uiState.value.isIssuing)
        assertEquals(issued, viewModel.uiState.value.invite)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isIssuing)
        assertEquals(TestInviteRepository.NEW_INVITE, viewModel.uiState.value.invite)
    }

    @Test
    fun 발급_중에_리셋하기를_다시_눌러도_한_번만_발급한다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(current = issued)
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        assertEquals(1, repository.createCount)
    }

    @Test
    fun 발급에_실패하면_기존_초대를_그대로_두고_오류를_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(current = issued, result = Result.failure(IOException()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        assertEquals(issued, viewModel.uiState.value.invite)
        assertFalse(viewModel.uiState.value.isIssuing)
        assertEquals(InviteLinkUiEffect.ShowInviteError, viewModel.effect.first())
        assertFalse(viewModel.uiState.value.isReissueFailed)
    }

    @Test
    fun 이전_초대를_지운_뒤_재발급에_실패하면_초대를_비우고_재발급_실패를_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(
            current = issued,
            result = Result.failure(InviteReissueFailedException(IOException())),
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.invite)
        assertTrue(viewModel.uiState.value.isReissueFailed)
        assertFalse(viewModel.uiState.value.canShare)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 재발급에_실패한_뒤_다시_리셋해서_발급하면_재발급_실패를_지운다() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = TestInviteRepository(
            current = issued,
            result = Result.failure(InviteReissueFailedException(IOException())),
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        repository.result = Result.success(TestInviteRepository.NEW_INVITE)
        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        advanceUntilIdle()

        assertEquals(TestInviteRepository.NEW_INVITE, viewModel.uiState.value.invite)
        assertFalse(viewModel.uiState.value.isReissueFailed)
    }

    @Test
    fun 코드와_링크를_복사하면_각각_원래_값을_복사한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(TestInviteRepository(current = issued))
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.CodeCopyClicked)
        assertEquals(InviteLinkUiEffect.CopyToClipboard("가족코드", "NWTKYJ"), viewModel.effect.first())

        viewModel.onEvent(InviteLinkUiEvent.LinkCopyClicked)
        assertEquals(
            InviteLinkUiEffect.CopyToClipboard("초대링크", "https://kiri.example.com/invite/NWTKYJ"),
            viewModel.effect.first(),
        )
    }

    @Test
    fun 공유_시트가_열려_있는_동안_다시_눌러도_한_번만_공유한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(TestInviteRepository(current = issued))
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ShareClicked)
        viewModel.onEvent(InviteLinkUiEvent.ShareClicked)

        assertEquals(InviteLinkUiEffect.ShareInvite(issued.url, issued.code), viewModel.effect.first())
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })

        // 공유 시트에서 돌아오면 다시 공유할 수 있다
        viewModel.onEvent(InviteLinkUiEvent.InviteShared)
        viewModel.onEvent(InviteLinkUiEvent.ShareClicked)
        assertEquals(InviteLinkUiEffect.ShareInvite(issued.url, issued.code), viewModel.effect.first())
    }

    @Test
    fun 발급_중에는_뒤로가기를_무시하고_평소에는_마이페이지로_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(TestInviteRepository(current = issued))
        advanceUntilIdle()

        viewModel.onEvent(InviteLinkUiEvent.ResetClicked)
        runCurrent()
        viewModel.onEvent(InviteLinkUiEvent.BackClicked)
        advanceUntilIdle()
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })

        viewModel.onEvent(InviteLinkUiEvent.BackClicked)
        assertEquals(InviteLinkUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 링크는_코드를_가려서_보여주고_만료일을_한국식으로_보여준다() {
        val state = InviteLinkUiState(invite = issued)

        assertEquals("kiri.example.com/invite/••••••", state.maskedLink)
        assertEquals("10월 12일까지 사용가능", state.expiresText)
        assertTrue(state.canShare)
    }

    @Test
    fun 초대가_없거나_발급_중이면_공유할_수_없다() {
        assertFalse(InviteLinkUiState().canShare)
        assertFalse(InviteLinkUiState(invite = issued, isIssuing = true).canShare)
        assertNull(InviteLinkUiState(invite = issued.copy(expiresAt = null)).expiresText)
    }
}
