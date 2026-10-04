package com.kiri.feature.onboarding

import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingViewModelTest {

    private val viewModel = OnboardingViewModel()
    private val state get() = viewModel.uiState.value

    private fun moveToOnboardingMain() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
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
}
