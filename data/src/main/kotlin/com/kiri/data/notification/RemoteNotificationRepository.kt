package com.kiri.data.notification

import com.kiri.data.network.ApiResponse
import com.kiri.domain.notification.FamilyNotification
import com.kiri.domain.notification.NotificationCategory
import com.kiri.domain.notification.NotificationRepository
import com.kiri.domain.notification.NotificationTarget
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries
import javax.inject.Singleton
import retrofit2.HttpException
import retrofit2.Retrofit

internal class RemoteNotificationRepository(private val api: NotificationApi) : NotificationRepository {
    override suspend fun load(): List<FamilyNotification> {
        val notifications = linkedMapOf<Long, FamilyNotification>()
        var page = 0
        do {
            val response = api.list(page++, PAGE_SIZE).requireSuccess()
            val result = checkNotNull(response.result)
            result.content.forEach {
                if (it.notificationId !in notifications) notifications[it.notificationId] = it.toDomain()
            }
        } while (!result.last && result.content.isNotEmpty())
        return notifications.values.toList()
    }

    override suspend fun markRead(id: String) {
        api.markRead(id.toLong()).requireSuccess()
    }

    override suspend fun markAllRead() {
        try {
            api.markAllRead().requireSuccess()
        } catch (error: HttpException) {
            if (error.code() != 404 && error.code() != 405) throw error
            // The linked Swagger also describes deployments with only the per-item endpoint.
            load().filterNot { it.read }.forEach { markRead(it.id) }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

private fun <T> ApiResponse<T>.requireSuccess(): ApiResponse<T> = apply {
    check(isSuccess) { code }
}

private val serverZone = ZoneId.of("Asia/Seoul")

private fun NotificationResponse.toDomain(): FamilyNotification {
    val type = InboxType.entries.firstOrNull { it.name == notificationType }
    val category = NotificationCategory.entries.firstOrNull { it.name == this.category }
        ?: type?.category ?: NotificationCategory.ALL
    val target = when (targetType) {
        null -> type?.target ?: NotificationTarget.UNSUPPORTED
        "QUESTION" -> NotificationTarget.QUESTION
        "FAMILY_ANSWERS" -> NotificationTarget.FAMILY_ANSWERS
        "WEEKLY_MISSION", "MISSION" -> NotificationTarget.MISSION
        "SCHEDULE_INVITATION" -> NotificationTarget.SCHEDULE_INVITATION
        "SCHEDULE" -> NotificationTarget.SCHEDULE
        "SCHEDULE_RECORD", "REVIEW_WRITE" -> NotificationTarget.MEMORY
        else -> NotificationTarget.UNSUPPORTED
    }.let {
        // 일정 변경알림은 일정 상세가 아니라 응답 화면(변경알림)으로 연다. 응답할 수 없는 일정이면 그 화면이 상세를 보여준다
        if (it == NotificationTarget.SCHEDULE && type in RESPOND_SCHEDULE_TYPES) NotificationTarget.SCHEDULE_INVITATION else it
    }
    val timestamp = DateTimeFormatter.ISO_DATE_TIME.parse(createdAt)
    val localTime = LocalDateTime.from(timestamp)
    val instant = timestamp.query(TemporalQueries.offset())?.let(localTime::toInstant)
        ?: localTime.atZone(serverZone).toInstant()
    return FamilyNotification(
        id = notificationId.toString(),
        sender = "",
        title = title,
        message = body,
        createdAt = instant,
        category = category,
        target = target,
        targetId = referenceId?.toString().orEmpty(),
        read = isRead,
        targetAccessible = referenceId != null && target != NotificationTarget.UNSUPPORTED,
    )
}

private val RESPOND_SCHEDULE_TYPES = setOf(InboxType.SCHEDULE_CHANGED, InboxType.SCHEDULE_RECONFIRM_REMINDER)

private enum class InboxType(val category: NotificationCategory, val target: NotificationTarget) {
    QUESTION_REMINDER(NotificationCategory.QUESTION, NotificationTarget.QUESTION),
    QUESTION_COMPLETE(NotificationCategory.QUESTION, NotificationTarget.FAMILY_ANSWERS),
    POKE(NotificationCategory.POKE, NotificationTarget.QUESTION),
    MISSION(NotificationCategory.MISSION, NotificationTarget.MISSION),
    MISSION_CHANGED(NotificationCategory.MISSION, NotificationTarget.MISSION),
    EXTERNAL_EVENT_CANCELLED(NotificationCategory.SCHEDULE, NotificationTarget.MISSION),
    SCHEDULE(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_INVITATION(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE_INVITATION),
    SCHEDULE_INVITATION_REMINDER(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE_INVITATION),
    SCHEDULE_RESPONSE(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_CHANGED(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_REMOVED(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_RECONFIRM_REMINDER(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_CANCELLED(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_NOT_CONFIRMED(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_D1_REMINDER(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    SCHEDULE_D0_REMINDER(NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE),
    RECORD(NotificationCategory.RECORD, NotificationTarget.MEMORY),
    RECORD_CREATED(NotificationCategory.RECORD, NotificationTarget.MEMORY),
    WEEKLY_MISSION_COMPLETED(NotificationCategory.RECORD, NotificationTarget.UNSUPPORTED),
}

@Module
@InstallIn(SingletonComponent::class)
internal object NotificationRepositoryModule {
    @Provides
    @Singleton
    fun provideNotificationRepository(retrofit: Retrofit): NotificationRepository =
        RemoteNotificationRepository(retrofit.create(NotificationApi::class.java))
}
