package com.kiri.feature.schedule

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionContent
import com.kiri.domain.place.*
import com.kiri.domain.schedule.*
import com.kiri.uikit.KiriTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*

class ScheduleScreenTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-10-18T03:00:00Z")
    private val family = FamilyHome(LocalDate.of(2026,10,18), "me", "가족", listOf(FamilyMember("me", "나", "아들"), FamilyMember("mother", "마마", "엄마")), null, emptyMap(), emptySet(), null)
    private val mission = MissionContent(PlaceMission("river", "한강 축제", "한강", "서울", PlaceCategory.OUTDOOR))
    @Test fun formRequiresInviteesAndFamilyCardChangesSelection() {
        var selected: Set<String> = emptySet()
        compose.setContent { KiriTheme { ScheduleScreen(ScheduleUiState(loading = false, family = family, mission = mission,
            draft = ScheduleDraft("bucket", "river", now.plusSeconds(86400), place = "한강"), editing = true, now = now), {}, { selected = it.inviteeIds }, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("schedule_preview").assertIsNotEnabled()
        compose.onNodeWithTag("schedule_invitee_mother").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(setOf("mother"), selected) }
    }
    @Test fun personalPendingResponseRemainsSeparateFromConfirmedFamilySchedule() {
        var response: MemberResponse? = null
        val schedule = FamilySchedule("schedule", "bucket", "river", "한강 축제", "mother", now.plusSeconds(172800), now.plusSeconds(86400),
            mapOf("mother" to MemberResponse.ACCEPTED, "father" to MemberResponse.ACCEPTED, "me" to MemberResponse.PENDING), "한강")
        compose.setContent { KiriTheme { ScheduleScreen(ScheduleUiState(loading = false, family = family, mission = mission, schedule = schedule, now = now), {}, {}, { _, _ -> }, {}, {}, { response = it }, {}, {}, {}, {}) } }
        compose.onNodeWithTag("schedule_status").assertTextEquals("약속 확정")
        compose.onNodeWithTag("schedule_personal_response").assertTextEquals("내 응답: 미응답")
        compose.onNodeWithTag("schedule_decline").performClick()
        compose.runOnIdle { assertEquals(MemberResponse.DECLINED, response) }
    }
}
