package com.kiri.domain.notification

import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeSchedule
import com.kiri.domain.mission.MissionContent

data class ScheduleInvitation(
    val notificationId: String,
    val kind: InvitationKind,
    val schedule: HomeSchedule,
    val mission: MissionContent,
    val members: List<FamilyMember>,
    val currentUserId: String,
) {
    val accepted: Boolean get() = currentUserId in schedule.acceptedMemberIds ||
        (schedule.finalParticipant && schedule.status == com.kiri.domain.home.ScheduleStatus.CONFIRMED)
}

interface ScheduleInvitationRepository {
    suspend fun load(notificationId: String): ScheduleInvitation
    suspend fun confirm(notificationId: String): ScheduleInvitation
}
