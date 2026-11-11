package com.kiri.feature.mypage

import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember as HomeMember
import com.kiri.domain.home.HomeRepository
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.family.FamilyMember
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// 가족 기본 설정과 마이페이지 프로필이 가족 정보 API(HomeRepository.family) 로 채워지는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class FamilySettingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class Home(var result: Result<FamilyHome>) : HomeRepository {
        var familyCalls = 0
        override suspend fun load(): FamilyHome = error("가족 정보만 필요한 화면은 홈 전체를 부르지 않는다")
        override suspend fun family(): FamilyHome { familyCalls++; return result.getOrThrow() }
        override suspend fun saveAnswer(question: DailyQuestion, answer: String) = error("unused")
        override suspend fun poke(question: DailyQuestion, memberId: String) = error("unused")
    }

    private fun family(name: String? = "끼리네가족") = FamilyHome(
        LocalDate.of(2026, 11, 10), "2", name,
        listOf(HomeMember("1", "아빠다", "아빠"), HomeMember("2", "쥬123", "딸"), HomeMember("3", "막내", ""),
            HomeMember("4", "탈퇴", "엄마", accessible = false)),
        null, emptyMap(), emptySet(), null, serverBacked = true,
    )

    @Test
    fun 가족_이름과_구성원을_불러오고_내_카드를_맨_앞에_둔다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = Home(Result.success(family()))
        val viewModel = FamilySettingViewModel(home)

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals("끼리네가족", state.familyName)
        assertEquals(
            listOf(
                FamilyMember("딸", "쥬123", isMe = true, id = "2"),
                FamilyMember("아빠", "아빠다", id = "1"),
                FamilyMember("", "막내", id = "3"),
            ),
            state.members,
        )
        assertEquals(MypageProfile(nickname = "쥬123", familyName = "끼리네가족", role = "딸"), state.toProfile())
        assertEquals(1, home.familyCalls)
    }

    @Test
    fun 가족명은_방장만_바꿀_수_있고_방장을_모르면_막지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = Home(Result.success(family().copy(ownerId = "1")))
        val viewModel = FamilySettingViewModel(home)
        viewModel.load(); advanceUntilIdle()
        // 나는 2번, 방장은 1번
        assertFalse(viewModel.uiState.value.canRenameFamily)

        home.result = Result.success(family().copy(ownerId = "2"))
        viewModel.load(); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canRenameFamily)

        home.result = Result.success(family().copy(ownerId = null))
        viewModel.load(); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canRenameFamily)
    }

    @Test
    fun 불러오지_못하면_실패로_두고_다시_시도하면_채운다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = Home(Result.failure(IOException()))
        val viewModel = FamilySettingViewModel(home)

        viewModel.load()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.loading)

        home.result = Result.success(family())
        viewModel.load()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.failed)
        assertEquals(3, viewModel.uiState.value.members.size)
    }

    @Test
    fun 다시_불러오는_동안_보이던_값을_지우지_않고_중복_요청하지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = Home(Result.success(family()))
        val viewModel = FamilySettingViewModel(home)
        viewModel.load(); advanceUntilIdle()

        home.result = Result.success(family(name = "새이름"))
        viewModel.load(); viewModel.load()
        assertFalse(viewModel.uiState.value.loading)
        assertEquals("끼리네가족", viewModel.uiState.value.familyName)
        advanceUntilIdle()

        assertEquals("새이름", viewModel.uiState.value.familyName)
        assertEquals(2, home.familyCalls)
    }

    @Test
    fun 가족이_없으면_가족_이름이_없고_프로필도_비어_있다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = FamilySettingViewModel(Home(Result.success(family(name = null).copy(members = emptyList()))))

        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.familyName)
        assertTrue(viewModel.uiState.value.members.isEmpty())
        assertEquals(MypageProfile("", "", ""), viewModel.uiState.value.toProfile())
    }
}
