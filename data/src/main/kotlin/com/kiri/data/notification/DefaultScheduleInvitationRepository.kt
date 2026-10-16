package com.kiri.data.notification

import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.notification.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Joins the inbox reference to canonical schedule and mission content; owns no duplicate schedule state. */
class DefaultScheduleInvitationRepository(
    private val notifications: NotificationRepository,
    private val home: HomeRepository,
    private val missions: MissionRepository,
) : ScheduleInvitationRepository {
    override suspend fun load(notificationId: String): ScheduleInvitation {
        val notification = notifications.load().firstOrNull { it.id == notificationId }
            ?: throw HomeException(HomeFailure.UNAVAILABLE)
        val kind = notification.invitationKind ?: throw HomeException(HomeFailure.UNAVAILABLE)
        if (notification.target != NotificationTarget.SCHEDULE || !notifications.canOpen(notification)) throw HomeException(HomeFailure.UNAVAILABLE)
        val family = home.load()
        val schedule = family.schedules.firstOrNull { it.id == notification.targetId }
            ?: throw HomeException(HomeFailure.UNAVAILABLE)
        if (family.browsing || family.members.none { it.id == family.currentUserId && it.accessible } ||
            notification.scheduleStartsAt != schedule.startsAt || schedule.status in setOf(ScheduleStatus.CANCELLED, ScheduleStatus.COMPLETED, ScheduleStatus.NOT_CONFIRMED)) {
            throw HomeException(HomeFailure.UNAVAILABLE)
        }
        return ScheduleInvitation(notificationId, kind, schedule, missions.load(schedule.missionId),
            family.members.filter { it.accessible }, family.currentUserId)
    }
    override suspend fun confirm(notificationId: String): ScheduleInvitation {
        val invitation = load(notificationId)
        home.confirmSchedule(invitation.schedule.id, invitation.schedule.startsAt)
        return load(notificationId)
    }
}

@Module @InstallIn(SingletonComponent::class)
object ScheduleInvitationRepositoryModule {
    @Provides @Singleton fun provideScheduleInvitationRepository(notifications: NotificationRepository,
        home: HomeRepository, missions: MissionRepository): ScheduleInvitationRepository =
        DefaultScheduleInvitationRepository(notifications, home, missions)
}
