package com.kiri.feature.record.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.kiri.domain.detail.Condition
import com.kiri.feature.detail.presentation.ConditionCustomScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.model.DetailUiState
import com.kiri.feature.record.ServerScheduleDetailUiState
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
}
