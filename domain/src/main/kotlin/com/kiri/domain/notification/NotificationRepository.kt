package com.kiri.domain.notification

import java.time.Instant

enum class NotificationCategory(val label: String) {
    ALL("받은 메일함"), SCHEDULE("일정 알림"), POKE("콕 알림"), RECORD("기록 알림"), MISSION("미션 알림")
}
enum class NotificationTarget { QUESTION, SCHEDULE, MISSION, MEMORY }
enum class InvitationKind { NEW, CHANGED }
data class FamilyNotification(
    val id: String,
    val sender: String,
    val title: String,
    val message: String,
    val createdAt: Instant,
    val category: NotificationCategory,
    val target: NotificationTarget,
    val targetId: String,
    val read: Boolean = false,
    val targetAccessible: Boolean = true,
    val invitationKind: InvitationKind? = null,
    val scheduleStartsAt: Instant? = null,
)
interface NotificationRepository {
    suspend fun load(): List<FamilyNotification>
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun canOpen(notification: FamilyNotification): Boolean = notification.targetAccessible
}
