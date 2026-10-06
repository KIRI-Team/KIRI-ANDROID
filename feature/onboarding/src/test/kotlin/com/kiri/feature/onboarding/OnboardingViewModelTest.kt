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

    @Test
    fun 닉네임이_규칙에_맞지_않으면_다음을_눌러도_넘어가지_않고_위반한_규칙을_표시한다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주!"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), state.nicknameViolations)
    }

    @Test
    fun 닉네임_오류는_다음을_누르기_전에는_표시하지_않는다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주"))

        assertEquals(emptySet<NicknameRule>(), state.nicknameViolations)
    }

    @Test
    fun 오류가_난_뒤_닉네임을_수정하면_오류가_사라진다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연"))

        assertEquals(emptySet<NicknameRule>(), state.nicknameViolations)
    }

    @Test
    fun 닉네임이_규칙에_맞으면_다음을_눌렀을_때_역할_선택으로_넘어간다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(emptySet<NicknameRule>(), state.nicknameViolations)
    }

    @Test
    fun 가족명이_규칙에_맞지_않으면_다음을_눌러도_넘어가지_않고_위반한_규칙을_표시한다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("!"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
        assertEquals(setOf(NicknameRule.LENGTH, NicknameRule.NO_SPECIAL_CHARACTERS), state.familyNameViolations)
    }

    @Test
    fun 오류가_난_뒤_가족명을_수정하면_오류가_사라진다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("!"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리네"))

        assertEquals(emptySet<NicknameRule>(), state.familyNameViolations)
    }

    @Test
    fun 닉네임이_10자를_넘으면_그대로_저장하고_다음을_누르면_길이_오류를_표시한다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("가".repeat(11)))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals("가".repeat(11), state.nickname)
        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertEquals(setOf(NicknameRule.LENGTH), state.nicknameViolations)
    }

    private fun moveToChooseRole() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
    }

    @Test
    fun 역할을_선택해도_다음을_누르기_전에는_넘어가지_않는다() {
        moveToChooseRole()

        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(FamilyRole.DAD, state.selectedRole)
    }

    @Test
    fun 역할을_선택하지_않으면_다음을_눌러도_넘어가지_않는다() {
        moveToChooseRole()

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
    }

    @Test
    fun 역할을_바꾼_뒤_다음을_누르면_마지막_역할로_온보딩_메인에_넘어간다() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))

        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.MOM))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(FamilyRole.MOM, state.selectedRole)
        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
    }
}
