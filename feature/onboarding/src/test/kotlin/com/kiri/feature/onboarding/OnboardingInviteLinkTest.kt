package com.kiri.feature.onboarding

import com.kiri.domain.model.family.ExpiredInviteCodeException
import com.kiri.domain.repository.PendingInviteRepository
import com.kiri.domain.usecase.invite.AcceptPendingInviteUseCase
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// 초대 링크로 받아 둔 토큰이 있으면, 닉네임·역할을 정하고 가족 선택 화면에 들어올 때 그 가족에 참여한다
class OnboardingInviteLinkTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class Pending(var token: String?) : PendingInviteRepository {
        override suspend fun save(token: String) { this.token = token }
        override suspend fun get(): String? = token
        override suspend fun clear() { token = null }
    }

    private val families = TestFamilyRepository()
    private val pending = Pending("tok-123")
    private val viewModel = OnboardingViewModel(
        families,
        TestMemberRepository(),
        acceptInvite = AcceptPendingInviteUseCase(pending, families),
    )

    private fun reachOnboardingMain() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
    }

    @Test
    fun 가족_선택_화면에_들어오면_받아_둔_토큰으로_참여하고_메인으로_간다() = runTest(mainDispatcherRule.testDispatcher) {
        reachOnboardingMain()
        advanceUntilIdle()

        assertEquals(listOf("tok-123"), families.sentTokens)
        assertNull(pending.token)
        assertEquals(
            listOf(OnboardingUiEffect.ShowMessage("끼리네가족에 참여했어요."), OnboardingUiEffect.NavigateToMainFlow),
            viewModel.effect.take(2).toList(),
        )
    }

    @Test
    fun 참여하지_못하면_이유를_알리고_가족_선택_화면에_머문다() = runTest(mainDispatcherRule.testDispatcher) {
        families.tokenJoinResult = Result.failure(ExpiredInviteCodeException())
        reachOnboardingMain()
        advanceUntilIdle()

        assertEquals(OnboardingStep.ONBOARDING_MAIN, viewModel.uiState.value.currentStep)
        assertEquals(false, viewModel.uiState.value.isSubmitting)
        assertNull(pending.token)
        assertEquals(
            listOf(OnboardingUiEffect.ShowMessage("만료된 초대 링크예요. 가족에게 새 링크를 받아주세요.")),
            viewModel.effect.take(1).toList(),
        )
    }

    @Test
    fun 받아_둔_초대가_없으면_평소처럼_가족_선택_화면에_머문다() = runTest(mainDispatcherRule.testDispatcher) {
        pending.token = null
        reachOnboardingMain()
        advanceUntilIdle()

        assertEquals(OnboardingStep.ONBOARDING_MAIN, viewModel.uiState.value.currentStep)
        assertTrue(families.sentTokens.isEmpty())
    }
}
