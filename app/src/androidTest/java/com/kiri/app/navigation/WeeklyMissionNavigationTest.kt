package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeRepositoryModule
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.mission.MissionRepositoryModule
import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.schedule.ScheduleRepository
import com.kiri.uikit.KiriTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.*
import dagger.hilt.components.SingletonComponent
import java.time.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

@HiltAndroidTest
@UninstallModules(HomeRepositoryModule::class, MissionRepositoryModule::class)
class WeeklyMissionNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Inject lateinit var buckets: BucketRepository
    @Inject lateinit var schedules: ScheduleRepository
    @Inject lateinit var home: HomeRepository
    @Before fun setup() { hilt.inject() }

    @Test fun homeToExistingFormCreatesBucketOnlyOnFinalSendAndReturnsToHome() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("미션 카드로 전환").performScrollTo().performClick()
        compose.onNodeWithText("일정 잡기").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_register_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("가족에게 추천된 주간 미션이에요!").assertIsDisplayed()
        assertTrue(runBlocking { buckets.mine().isEmpty() })
        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithText("일정 잡기").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_register_screen").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { buckets.mine().isEmpty() })

        val tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1)
        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        if (YearMonth.from(tomorrow) != YearMonth.now(ZoneId.of("Asia/Seoul")))
            compose.onNodeWithTag("record_calendar_next").performClick()
        compose.onNodeWithTag("record_calendar_day_$tomorrow").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_mother").performClick()
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithText("초대장 만들기").performClick()
        assertTrue(runBlocking { buckets.mine().isEmpty() })
        compose.onNodeWithText("요청 보내기").performClick()
        compose.onNodeWithTag("record_send_app").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_mission_card").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("일정 보기").performScrollTo().assertIsDisplayed()
        val weeklyId = runBlocking { home.load().mission!!.id }
        val activity = schedules.schedules.value.single { it.weeklyMissionId == weeklyId }
        assertEquals(setOf(activity.id), runBlocking { buckets.mine().single().linkedScheduleIds })
        compose.onNodeWithText("일정 보기").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_screen").fetchSemanticsNodes().isNotEmpty() }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {
        @Provides fun clock(): Clock = Clock.systemUTC()
        @Provides fun tutorial(): HomeTutorialRepository = NoHomeTutorial
        @Provides @Singleton fun missions(): MissionRepository = FakeMissionRepository()
        @Provides @Singleton fun home(clock: Clock): HomeRepository {
            val source = FakeHomeRepository(clock, latencyMillis = 0)
            val identity = UUID.randomUUID().toString()
            return object : HomeRepository by source {
                override suspend fun load(): FamilyHome {
                    val loaded = source.load()
                    return loaded.copy(familyName = "주간 미션 테스트 $identity", schedules = emptyList(),
                        mission = loaded.mission?.copy(id = identity))
                }
                // by source 로 위임하면 family() 가 source 의 load() 로 가서 위에서 바꾼 load() 를 거치지 않는다. 가족 정보도 바꾼 load() 로 준다
                override suspend fun family(): FamilyHome = load()
            }
        }
    }
}
