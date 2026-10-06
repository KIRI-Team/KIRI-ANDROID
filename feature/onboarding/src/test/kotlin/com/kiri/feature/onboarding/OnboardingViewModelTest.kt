package com.kiri.feature.onboarding

import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = OnboardingViewModel()
    private val state get() = viewModel.uiState.value

    private fun moveToOnboardingMain() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
    }

    private fun moveToLoading() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리네가족"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
    }

    @Test
    fun 코드_생성_로딩은_1초_뒤에_코드_생성_완료_화면으로_넘어간다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToLoading()
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE_LOADING, state.currentStep)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE_LOADING, state.currentStep)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE, state.currentStep)
    }

    @Test
    fun 로딩_중에_뒤로가면_자동으로_넘어가지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToLoading()

        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
    }

    @Test
    fun 가족공간_만들기를_선택하면_가족명_입력으로_이동한다() {
        moveToOnboardingMain()

        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
    }

    @Test
    fun 가족명을_입력하면_상태에_반영된다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))

        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리네가족"))

        assertEquals("끼리네가족", state.familyName)
    }

    @Test
    fun 가족코드_입력하기를_선택하면_가족코드_입력으로_이동하고_입력값이_반영된다() {
        moveToOnboardingMain()

        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.InputFamilyCodeChanged("NWTKYJ"))

        assertEquals(OnboardingStep.INPUT_FAMILY_CODE, state.currentStep)
        assertEquals("NWTKYJ", state.inputFamilyCode)
    }

    @Test
    fun 가족코드_입력에서_뒤로가면_온보딩_메인으로_돌아간다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY))

        viewModel.onEvent(OnboardingUiEvent.BackClicked)

        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
    }

    @Test
    fun 가족명_입력에서_뒤로가면_온보딩_메인으로_돌아간다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))

        viewModel.onEvent(OnboardingUiEvent.BackClicked)

        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
    }

    @Test
    fun 코드_생성_완료_화면에서_초대하기를_누르면_메인으로_이동하는_효과를_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToLoading()
        advanceUntilIdle()
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE, state.currentStep)

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        assertEquals(OnboardingUiEffect.NavigateToMainFlow, viewModel.effect.first())
    }

    @Test
    fun 가족코드_입력_화면에서_초대하기를_누르면_메인으로_이동하는_효과를_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY))
        assertEquals(OnboardingStep.INPUT_FAMILY_CODE, state.currentStep)

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        assertEquals(OnboardingUiEffect.NavigateToMainFlow, viewModel.effect.first())
    }
}
