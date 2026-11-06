package com.kiri.feature.record.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import com.kiri.domain.detail.Condition
import com.kiri.feature.detail.presentation.ConditionCustomScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionGuide
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.mission.MissionPlace
import com.kiri.domain.model.mission.WalkingLevel
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.model.DetailUiState
import com.kiri.feature.record.ServerScheduleDetailUiState
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.review.ReviewWriter
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.feature.record.toMissionDetail
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 서버 일정 상세 화면이 일정 상세 API 값으로 머리말을 채우는지 확인한다
class ServerScheduleDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    private val detail = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = ScheduleDisplayStatus.TODAY,
        participants = listOf(ScheduleParticipant("엄마", true), ScheduleParticipant("첫째", false)),
        missionId = 42,
    )

    @Test
    fun showsDateStatusTimeAndParticipantsFromScheduleDetail() {
        compose.setContent {
            KiriTheme {
                ServerScheduleDetailScreen(
                    ServerScheduleDetailUiState(
                        detail = DetailUiState(loading = false, detail = detail.toMissionDetail(7)),
                        schedule = detail,
                    ),
                    {},
                )
            }
        }
        compose.onNodeWithText("11월 17일").assertIsDisplayed()
        compose.onNodeWithText("약속 확정").assertIsDisplayed()
        compose.onNodeWithText("11월 17일 약속").assertIsDisplayed()
        compose.onNodeWithText("엄마").assertIsDisplayed()
        compose.onNodeWithText("첫째").assertIsDisplayed()
        compose.onNodeWithText("미참여").assertIsDisplayed()
        compose.onNodeWithText("시작 시간   I   오후 2시 50분").assertIsDisplayed()
        compose.onNodeWithText("장소   I   미정").assertIsDisplayed()
    }

    @Test
    fun missionFillsTitlePlaceWalkingDurationActivitiesAndEssentials() {
        val mission = MissionInfo(
            id = 42, title = "국립현대미술관 서울 관람하기", description = null, category = null, missionType = null,
            status = null, indoorOutdoor = null, imageUrl = null, imageSource = null, startDate = null, endDate = null,
            openingTime = null, closingTime = null, price = 0, durationMinMinutes = 60, durationMaxMinutes = 120,
            walkingLevel = WalkingLevel.MEDIUM, sourceUrl = null,
            place = MissionPlace(52, "국립현대미술관 서울", null, null, null),
            prepareItems = listOf(MissionGuide("사전 예약", "일부 전시 예약 필요")),
            steps = listOf(MissionGuide("상설 전시 관람하기", "마음에 드는 작품 고르기")),
            bucketState = MissionBucketState.NONE, averageRating = null, reviewCount = 0,
        )
        compose.setContent {
            KiriTheme {
                ServerScheduleDetailScreen(
                    ServerScheduleDetailUiState(
                        detail = DetailUiState(loading = false, detail = detail.toMissionDetail(7, mission)),
                        schedule = detail,
                    ),
                    {},
                )
            }
        }

        compose.onNodeWithText("국립현대미술관 서울 관람하기").assertIsDisplayed()
        compose.onNodeWithText("장소   I   국립현대미술관 서울").assertIsDisplayed()
        compose.onNodeWithText("예상 걸음수 I 보통").assertIsDisplayed()
        compose.onNodeWithText("활동시간 I 1시간 ~ 2시간").assertIsDisplayed()
        compose.onNodeWithText("상설 전시 관람하기").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("사전 예약").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun customButtonShowsOnlyOnTheDayAndSendsCustomClicked() {
        val events = mutableListOf<DetailUiEvent>()
        var schedule by mutableStateOf(detail)
        compose.setContent {
            KiriTheme {
                ServerScheduleDetailScreen(
                    ServerScheduleDetailUiState(
                        detail = DetailUiState(loading = false, detail = schedule.toMissionDetail(7)),
                        schedule = schedule,
                    ),
                    { events += it },
                )
            }
        }
        compose.onNodeWithText("컨디션 조정하러가기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf<DetailUiEvent>(DetailUiEvent.CustomClicked), events) }

        schedule = detail.copy(status = ScheduleDisplayStatus.CONFIRMED)
        compose.onNodeWithText("컨디션 조정하러가기").assertDoesNotExist()
    }

    @Test
    fun customScreenLetsConditionsBePickedButApplyIsDisabled() {
        val events = mutableListOf<DetailUiEvent>()
        compose.setContent {
            KiriTheme {
                ConditionCustomScreen(
                    DetailUiState(loading = false, detail = detail.toMissionDetail(7)),
                    { events += it },
                    applyEnabled = false,
                )
            }
        }
        compose.onNodeWithTag("condition_MORE_REST").performClick()
        compose.onNodeWithTag("apply_custom").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf<DetailUiEvent>(DetailUiEvent.ConditionToggled(Condition.MORE_REST)), events)
        }
    }

    @Test
    fun scheduledShowsParticipantCheckTooltipInsteadOfParticipantRow() {
        val scheduled = detail.copy(status = ScheduleDisplayStatus.SCHEDULED, date = LocalDate.of(2026, 6, 13))
        compose.setContent {
            KiriTheme {
                ServerScheduleDetailScreen(
                    ServerScheduleDetailUiState(
                        detail = DetailUiState(loading = false, detail = scheduled.toMissionDetail(7)),
                        schedule = scheduled,
                    ),
                    {},
                )
            }
        }
        compose.onNodeWithText("약속 예정").assertIsDisplayed()
        compose.onNodeWithText("  I  2026.06.13").assertIsDisplayed()
        // 참여 상태 줄은 숨기고, 인원확인을 눌러야 참여자가 보인다
        compose.onNodeWithText("참여상태  I").assertDoesNotExist()
        compose.onNodeWithText("엄마").assertDoesNotExist()

        compose.onNodeWithTag("participant_check").performClick()
        compose.onNodeWithTag("participant_check_tooltip").assertIsDisplayed()
        compose.onNodeWithText("엄마", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("첫째", useUnmergedTree = true).assertIsDisplayed()
        // 말풍선에는 이름 아래 상태 문구를 그리지 않는다
        compose.onNodeWithText("미수락", useUnmergedTree = true).assertDoesNotExist()

        compose.onNodeWithTag("participant_check_dim").performTouchInput { click(Offset(10f, 10f)) }
        compose.onNodeWithTag("participant_check_tooltip").assertDoesNotExist()
    }

    @Test
    fun loadingAndFinalFailureUseDateTitleAndHideRetry() {
        var state by mutableStateOf(
            ServerScheduleDetailUiState(DetailUiState(loading = true), requestedDate = LocalDate.of(2026, 11, 17)),
        )
        compose.setContent { KiriTheme { ServerScheduleDetailScreen(state, {}) } }
        compose.onNodeWithText("11월 17일").assertIsDisplayed()
        compose.onNodeWithText("미션 상세").assertDoesNotExist()

        state = state.copy(detail = DetailUiState(loading = false, error = "취소된 약속이에요."), retryable = false)
        compose.onNodeWithText("11월 17일").assertIsDisplayed()
        compose.onNodeWithText("취소된 약속이에요.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
    }

    @Test
    fun failedLoadShowsMessageAndRetry() {
        val events = mutableListOf<DetailUiEvent>()
        compose.setContent {
            KiriTheme {
                ServerScheduleDetailScreen(
                    ServerScheduleDetailUiState(DetailUiState(loading = false, error = "약속 정보를 불러오지 못했어요.")),
                    { events += it },
                )
            }
        }
        compose.onNodeWithText("약속 정보를 불러오지 못했어요.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.runOnIdle { assertEquals(listOf<DetailUiEvent>(DetailUiEvent.RetryClicked), events) }
    }

    @Test
    fun completedScheduleShowsRecordScreenWithFamilyReviews() {
        val completed = detail.copy(status = ScheduleDisplayStatus.COMPLETED)
        var state by mutableStateOf(
            ServerScheduleDetailUiState(
                detail = DetailUiState(loading = false, detail = completed.toMissionDetail(7)),
                schedule = completed,
                familyName = "끼리네가족",
                reviews = listOf(
                    ScheduleReview(1, ReviewWriter(5, "김주연", FamilyRole.DAUGHTER), "처음 가봤다.", 5, true, emptyList(), null, isMine = true),
                    ScheduleReview(2, ReviewWriter(6, "아빠", null), "또 가요", 4, false, emptyList(), null, isMine = false),
                ),
            ),
        )
        val events = mutableListOf<DetailUiEvent>()
        var retried = 0
        compose.setContent { KiriTheme { ServerScheduleDetailScreen(state, { events += it }, { retried++ }) } }

        compose.onNodeWithTag("record_memory_screen").assertIsDisplayed()
        compose.onNodeWithText("끼리네가족 기록").assertIsDisplayed()
        compose.onNodeWithText("약속 완료").assertIsDisplayed()
        compose.onNodeWithText("2026.11.17").assertIsDisplayed()
        compose.onNodeWithText("끼리네가족님의 기록 모아보기").performScrollTo().assertIsDisplayed()
        // 역할을 아는 작성자는 "이름 I 역할", 모르면 이름만 쓴다
        compose.onNodeWithText("김주연 I 딸").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("아빠").performScrollTo().assertIsDisplayed()
        // 서버 리뷰는 아직 수정·삭제 API 가 없어 수정하기와 메뉴를 보여주지 않는다
        compose.onNodeWithTag("edit_record_1").assertDoesNotExist()
        compose.onNodeWithTag("record_memory_menu_1").assertDoesNotExist()
        // 대표 사진 변경도 아직 막아 둔다
        compose.onNodeWithTag("record_representative_photo").performScrollTo().performClick()
        compose.onNodeWithTag("change_record_photo").assertDoesNotExist()

        state = state.copy(reviews = emptyList(), reviewsFailed = true)
        compose.onNodeWithTag("record_memory_retry").performScrollTo().performClick()
        assertEquals(1, retried)

        state = state.copy(reviewsFailed = false)
        compose.onNodeWithTag("record_memory_empty").performScrollTo().assertIsDisplayed()

        compose.onNodeWithContentDescription("뒤로").performClick()
        assertEquals(listOf<DetailUiEvent>(DetailUiEvent.BackClicked), events)
    }
}
