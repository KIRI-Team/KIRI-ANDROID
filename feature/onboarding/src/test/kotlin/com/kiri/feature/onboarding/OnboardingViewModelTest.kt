package com.kiri.feature.onboarding

import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.nickname.DuplicateNicknameException
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.domain.model.invite.Invite
import java.io.IOException
import com.kiri.domain.model.member.FamilyRole as MemberFamilyRole
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val familyRepository = TestFamilyRepository()
    private val memberRepository = TestMemberRepository()
    private val viewModel = OnboardingViewModel(familyRepository, memberRepository)
    private val state get() = viewModel.uiState.value

    // 닉네임 저장처럼 viewModelScope 에서 시작한 작업을 지금 시점까지 실행한다
    private fun runPending() = mainDispatcherRule.testDispatcher.scheduler.runCurrent()

    private fun moveToOnboardingMain() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()
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
        assertEquals(familyRepository.result.getOrThrow().invite.code, state.familyCode)
    }

    @Test
    fun 가족명_입력에서_다음을_누르면_로딩_화면에서_입력한_가족명으로_가족_공간을_만든다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToLoading()
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE_LOADING, state.currentStep)

        runCurrent()

        assertEquals(listOf("끼리네가족"), familyRepository.sentNames)
    }

    @Test
    fun 가족명이_규칙에_맞지_않으면_가족_공간을_만들지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리!"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
        assertEquals(emptyList<String>(), familyRepository.sentNames)
    }

    @Test
    fun 로딩_중에는_뒤로가기를_무시하고_가족_공간을_다_만든_뒤_넘어간다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToLoading()

        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE_LOADING, state.currentStep)
        advanceUntilIdle()

        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE, state.currentStep)
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

    // 이후에 발생하는 효과를 모두 모은다
    private fun TestScope.collectEffects(): List<OnboardingUiEffect> {
        val effects = mutableListOf<OnboardingUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun TestScope.moveToGeneratedCode() {
        moveToLoading()
        advanceUntilIdle()
    }

    @Test
    fun 가족_공간을_만들며_발급된_초대_코드를_화면에_보여준다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()

        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE, state.currentStep)
        assertEquals("AB12CD", state.familyCode)
        assertEquals(1, familyRepository.sentNames.size)
    }

    @Test
    fun 가족_공간을_만들지_못하면_가족명_입력으로_돌아가고_오류를_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.result = Result.failure(IOException())
        val effects = collectEffects()

        moveToGeneratedCode()

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
        assertFalse(state.isFamilyNameDuplicated)
        assertEquals(listOf(OnboardingUiEffect.ShowCreateFamilyError), effects)
    }

    @Test
    fun 이미_존재하는_가족_이름이면_가족명_입력으로_돌아가_중복을_표시한다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.result = Result.failure(DuplicateFamilyNameException())
        val effects = collectEffects()

        moveToGeneratedCode()

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
        assertTrue(state.isFamilyNameDuplicated)
        assertEquals(emptyList<OnboardingUiEffect>(), effects)
    }

    @Test
    fun 가족명_중복_표시_후_가족명을_수정하면_중복_표시가_사라진다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.result = Result.failure(DuplicateFamilyNameException())
        moveToGeneratedCode()

        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리네집"))

        assertFalse(state.isFamilyNameDuplicated)
    }

    @Test
    fun 코드_생성_완료_화면에서_초대하기를_누르면_발급된_초대를_공유한다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        val effects = collectEffects()

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        assertEquals(listOf(OnboardingUiEffect.ShareInvite(familyRepository.result.getOrThrow().invite)), effects)
    }

    @Test
    fun 공유_시트가_열려_있는_동안_초대하기를_다시_눌러도_무시한다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        val effects = collectEffects()

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        assertEquals(1, effects.count { it is OnboardingUiEffect.ShareInvite })
    }

    @Test
    fun 공유_시트에서_돌아오면_메인으로_이동한다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        val effects = collectEffects()
        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        viewModel.onEvent(OnboardingUiEvent.InviteShared)

        assertEquals(OnboardingUiEffect.NavigateToMainFlow, effects.last())
    }

    @Test
    fun 공유하지_않았으면_공유_완료_이벤트를_무시한다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        val effects = collectEffects()

        viewModel.onEvent(OnboardingUiEvent.InviteShared)

        assertEquals(emptyList<OnboardingUiEffect>(), effects)
    }

    @Test
    fun 다시_공유해도_초대를_재발급하지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        val effects = collectEffects()
        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        viewModel.onEvent(OnboardingUiEvent.InviteShared)

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)

        assertEquals(2, effects.count { it is OnboardingUiEffect.ShareInvite })
        assertEquals(1, familyRepository.sentNames.size)
    }

    @Test
    fun 만든_가족명_그대로_다시_다음을_누르면_다시_만들지_않고_코드_화면으로_간다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE, state.currentStep)
        assertEquals("AB12CD", state.familyCode)
        assertEquals(1, familyRepository.sentNames.size)
    }

    @Test
    fun 가족명을_바꿔_다시_다음을_누르면_새_이름으로_다시_만든다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToGeneratedCode()
        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("끼리네집"))
        familyRepository.result = Result.success(
            Family(id = 2, name = "끼리네집", invite = Invite(code = "ZZ99ZZ", url = "https://kiri.app/invite/ZZ99ZZ")),
        )

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals("ZZ99ZZ", state.familyCode)
        assertEquals(listOf("끼리네가족", "끼리네집"), familyRepository.sentNames)
    }

    private fun moveToFamilyCode(code: String) {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.InputFamilyCodeChanged(code))
    }

    @Test
    fun 가족코드로_가입하면_입력한_코드를_보내고_메인으로_넘어간다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToFamilyCode("4QNC1Q")
        val effects = collectEffects()

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        advanceUntilIdle()

        assertEquals(listOf("4QNC1Q"), familyRepository.sentCodes)
        assertEquals(listOf<OnboardingUiEffect>(OnboardingUiEffect.NavigateToMainFlow), effects)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun 가족코드를_6자리_모두_입력하지_않으면_보내지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToFamilyCode("4QNC")
        val effects = collectEffects()

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        advanceUntilIdle()

        assertTrue(familyRepository.sentCodes.isEmpty())
        assertEquals(listOf<OnboardingUiEffect>(OnboardingUiEffect.ShowJoinFamilyError("6자리 가족코드를 모두 입력해주세요.")), effects)
    }

    @Test
    fun 가입하는_동안_다시_눌러도_한_번만_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToFamilyCode("4QNC1Q")

        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        assertTrue(state.isSubmitting)
        viewModel.onEvent(OnboardingUiEvent.InviteClicked)
        advanceUntilIdle()

        assertEquals(1, familyRepository.sentCodes.size)
    }

    @Test
    fun 가입하지_못하면_이유를_알리고_입력과_화면을_그대로_둔다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToFamilyCode("4QNC1Q")
        val effects = collectEffects()
        val cases = listOf(
            com.kiri.domain.model.family.InvalidInviteCodeException() to "가족코드를 다시 확인해주세요.",
            com.kiri.domain.model.family.ExpiredInviteCodeException() to "만료된 가족코드예요. 가족에게 새 코드를 받아주세요.",
            com.kiri.domain.model.family.AlreadyInFamilyException() to "이미 다른 가족에 가입되어 있어요.",
            IOException() to "가족에 참여하지 못했어요. 잠시 후 다시 시도해주세요.",
        )

        cases.forEach { (error, _) ->
            familyRepository.joinResult = Result.failure(error)
            viewModel.onEvent(OnboardingUiEvent.InviteClicked)
            advanceUntilIdle()
        }

        assertEquals(cases.map { OnboardingUiEffect.ShowJoinFamilyError(it.second) }, effects)
        assertEquals(OnboardingStep.INPUT_FAMILY_CODE, state.currentStep)
        assertEquals("4QNC1Q", state.inputFamilyCode)
        assertFalse(state.isSubmitting)
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
    fun 닉네임이_규칙에_맞으면_서버에_저장한_뒤_역할_선택으로_넘어간다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertTrue(state.isSubmitting)

        runPending()
        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertFalse(state.isSubmitting)
        assertEquals(listOf("주연주"), memberRepository.sentNicknames)
        assertEquals(emptySet<NicknameRule>(), state.nicknameViolations)
    }

    @Test
    fun 닉네임이_규칙에_맞지_않으면_서버에_보내지_않는다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주!"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        assertEquals(emptyList<String>(), memberRepository.sentNicknames)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun 저장하는_동안_다음을_다시_눌러도_한_번만_보낸다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        assertEquals(listOf("주연주"), memberRepository.sentNicknames)
        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
    }

    @Test
    fun 이미_사용_중인_닉네임이면_넘어가지_않고_중복을_표시한다() {
        memberRepository.result = Result.failure(DuplicateNicknameException())
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertTrue(state.isNicknameDuplicated)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun 중복_표시_후_닉네임을_수정하면_중복_표시가_사라진다() {
        memberRepository.result = Result.failure(DuplicateNicknameException())
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주2"))

        assertFalse(state.isNicknameDuplicated)
    }

    @Test
    fun 닉네임_저장에_실패하면_넘어가지_않고_오류를_알리며_다시_시도할_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        memberRepository.result = Result.failure(IOException())
        val effects = collectEffects()
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runCurrent()

        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertFalse(state.isNicknameDuplicated)
        assertEquals(listOf(OnboardingUiEffect.ShowNicknameError), effects)

        memberRepository.result = Result.success(Unit)
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runCurrent()

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(listOf("주연주", "주연주"), memberRepository.sentNicknames)
    }

    @Test
    fun 저장한_닉네임으로_돌아왔다가_그대로_다음을_누르면_다시_보내지_않는다() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.BackClicked)

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(listOf("주연주"), memberRepository.sentNicknames)
    }

    @Test
    fun 저장한_닉네임으로_돌아와_바꾸면_새_닉네임을_보낸다() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연"))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(listOf("주연주", "주연"), memberRepository.sentNicknames)
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
    fun 가족명은_닉네임보다_긴_15자까지_넘어간다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("가".repeat(15)))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.GENERATE_FAMILY_CODE_LOADING, state.currentStep)
        assertEquals(emptySet<NicknameRule>(), state.familyNameViolations)
    }

    @Test
    fun 가족명이_15자를_넘으면_길이_오류를_표시한다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY))
        viewModel.onEvent(OnboardingUiEvent.FamilyNameChanged("가".repeat(16)))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.INPUT_FAMILY_NAME, state.currentStep)
        assertEquals(setOf(NicknameRule.LENGTH), state.familyNameViolations)
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
    fun 닉네임이_12자를_넘으면_그대로_저장하고_다음을_누르면_길이_오류를_표시한다() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("가".repeat(13)))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals("가".repeat(13), state.nickname)
        assertEquals(OnboardingStep.INPUT_NICKNAME, state.currentStep)
        assertEquals(setOf(NicknameRule.LENGTH), state.nicknameViolations)
    }

    private fun moveToChooseRole() {
        viewModel.onEvent(OnboardingUiEvent.NicknameChanged("주연주"))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()
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
        runPending()

        assertEquals(FamilyRole.MOM, state.selectedRole)
        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
        assertEquals(listOf(MemberFamilyRole.MOTHER), memberRepository.sentRoles)
    }

    @Test
    fun 역할을_서버에_저장한_뒤_온보딩_메인으로_넘어간다() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertTrue(state.isSubmitting)

        runPending()
        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
        assertFalse(state.isSubmitting)
        assertEquals(listOf(MemberFamilyRole.FATHER), memberRepository.sentRoles)
    }

    @Test
    fun 화면의_역할을_서버의_역할_값으로_바꿔_보낸다() {
        val expected = mapOf(
            FamilyRole.MOM to MemberFamilyRole.MOTHER,
            FamilyRole.DAD to MemberFamilyRole.FATHER,
            FamilyRole.DAUGHTER to MemberFamilyRole.DAUGHTER,
            FamilyRole.SON to MemberFamilyRole.SON,
        )
        moveToChooseRole()

        expected.keys.forEach { role ->
            viewModel.onEvent(OnboardingUiEvent.RoleSelected(role))
            viewModel.onEvent(OnboardingUiEvent.NextClicked)
            runPending()
            viewModel.onEvent(OnboardingUiEvent.BackClicked)
        }

        assertEquals(expected.values.toList(), memberRepository.sentRoles)
    }

    @Test
    fun 역할_저장에_실패하면_넘어가지_않고_오류를_알리며_다시_시도할_수_있다() = runTest(mainDispatcherRule.testDispatcher) {
        moveToChooseRole()
        memberRepository.roleResult = Result.failure(IOException())
        val effects = collectEffects()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.SON))

        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runCurrent()

        assertEquals(OnboardingStep.CHOOSE_ROLE, state.currentStep)
        assertEquals(listOf(OnboardingUiEffect.ShowRoleError), effects)

        memberRepository.roleResult = Result.success(Unit)
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runCurrent()

        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
        assertEquals(listOf(MemberFamilyRole.SON, MemberFamilyRole.SON), memberRepository.sentRoles)
    }

    @Test
    fun 역할을_저장하는_동안에는_역할_변경과_뒤로가기와_다시_누르기를_무시한다() {
        moveToChooseRole()
        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.DAD))
        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        viewModel.onEvent(OnboardingUiEvent.RoleSelected(FamilyRole.MOM))
        viewModel.onEvent(OnboardingUiEvent.BackClicked)
        viewModel.onEvent(OnboardingUiEvent.NextClicked)
        runPending()

        assertEquals(FamilyRole.DAD, state.selectedRole)
        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
        assertEquals(listOf(MemberFamilyRole.FATHER), memberRepository.sentRoles)
    }

    @Test
    fun 저장한_역할로_돌아왔다가_그대로_다음을_누르면_다시_보내지_않는다() {
        moveToOnboardingMain()
        viewModel.onEvent(OnboardingUiEvent.BackClicked)

        viewModel.onEvent(OnboardingUiEvent.NextClicked)

        assertEquals(OnboardingStep.ONBOARDING_MAIN, state.currentStep)
        assertEquals(listOf(MemberFamilyRole.FATHER), memberRepository.sentRoles)
    }
}
