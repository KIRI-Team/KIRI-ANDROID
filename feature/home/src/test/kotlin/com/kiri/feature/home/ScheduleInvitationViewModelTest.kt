package com.kiri.feature.home

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionContent
import com.kiri.domain.notification.*
import com.kiri.domain.place.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleInvitationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun duplicateTapsAreIgnoredAndFailureCanBeRetriedWithoutLosingContent() = runTest(dispatcher) {
        val repo = Repository()
        val vm = ScheduleInvitationViewModel(repo, SavedStateHandle(mapOf("notificationId" to "invite")))
        advanceUntilIdle()
        repo.fail = true
        vm.confirm(); vm.confirm(); advanceUntilIdle()
        assertEquals(1, repo.submissions)
        assertNotNull(vm.uiState.value.invitation)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.confirmed)
        repo.fail = false
        vm.confirm(); advanceUntilIdle()
        assertTrue(vm.uiState.value.confirmed)
        vm.confirm(); advanceUntilIdle()
        assertEquals(2, repo.submissions)
    }
    private class Repository : ScheduleInvitationRepository {
        var fail = false
        var submissions = 0
        var invitation = ScheduleInvitation("invite", InvitationKind.CHANGED,
            HomeSchedule("existing", "미션", Instant.parse("2030-01-01T00:00:00Z"), ScheduleStatus.RECONFIRM_REQUIRED, false),
            MissionContent(PlaceMission("mission", "미션", "장소", "서울", PlaceCategory.OUTDOOR)), listOf(FamilyMember("me", "나", "아들")), "me")
        override suspend fun load(notificationId: String) = invitation
        override suspend fun confirm(notificationId: String): ScheduleInvitation {
            submissions++; delay(30)
            if (fail) throw HomeException(HomeFailure.NETWORK)
            invitation = invitation.copy(schedule = invitation.schedule.copy(status = ScheduleStatus.CONFIRMED, finalParticipant = true, acceptedMemberIds = setOf("me")))
            return invitation
        }
    }
}
