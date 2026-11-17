package com.kiri.data.notification

import com.kiri.domain.home.HomeRepository
import com.kiri.domain.notification.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Explicit demo inbox. Read state lives in this process; no push delivery is simulated. */
class FakeNotificationRepository(private val home: HomeRepository, private val clock: Clock = Clock.systemUTC()) : NotificationRepository {
    private val mutex = Mutex()
    private val readIds = mutableSetOf<String>()
    private var inbox: List<FamilyNotification>? = null
    override suspend fun load(): List<FamilyNotification> = mutex.withLock {
        val family = home.load()
        if (family.browsing) return@withLock emptyList()
        if (inbox == null) {
            val now = clock.instant()
            val schedule = family.schedules.firstOrNull()
            val mission = family.mission
            inbox = buildList {
                family.question?.let { question ->
                    add(FamilyNotification("demo-question", "끼리네", "[🧩] 아직 질문에 답변하지 않으셨어요!", "오늘 받은 질문카드는 오늘내로 답변해주셔야합니다", now.minus(10, ChronoUnit.HOURS), NotificationCategory.ALL, NotificationTarget.QUESTION, question.id, read = true))
                    add(FamilyNotification("demo-poke", "마마 (엄마)", "[콕] ‘마마’님이 나를 ‘콕’ 찔렀어요!", "마마님이 콕 찔렀어요. 들어가서 확인해주시고, 질문에 답변을 남겨주세요", now.minus(2, ChronoUnit.DAYS), NotificationCategory.POKE, NotificationTarget.QUESTION, question.id))
                }
                schedule?.let {
                    add(FamilyNotification("demo-invite", "마마 (엄마)", "[일정] 초대장이 도착했어요!", "엄마님이 나를 ‘${it.title}’에 초대했어요!", now.minus(3, ChronoUnit.DAYS), NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE, it.id))
                    add(FamilyNotification("demo-change", "끼리네", "[일정] ⚠ 주최측 일정으로 이번주 미션이 변경됐어요!", "중요한 일정 변경이 있어요! 들어가서 확인해주세요!", now.minus(4, ChronoUnit.DAYS), NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE, it.id))
                    add(FamilyNotification("demo-memory", "끼리네", "[기록] 마마님이 오늘 미션에 추억을 남겼어요!", "가족이 남긴 추억을 확인해보세요", now.minus(6, ChronoUnit.DAYS), NotificationCategory.RECORD, NotificationTarget.MEMORY, it.id))
                }
                mission?.let { add(FamilyNotification("demo-mission", "끼리네", "[미션] 이번주 미션이 오픈 됐어요! ♥", "1주일동안 모인 취향데이터 기반 AI 미션이 생성되었어요", now.minus(5, ChronoUnit.DAYS), NotificationCategory.MISSION, NotificationTarget.MISSION, it.id)) }
            }
        }
        inbox.orEmpty().map { it.copy(read = it.read || it.id in readIds) }.sortedByDescending { it.createdAt }
    }
    override suspend fun markRead(id: String) = mutex.withLock {
        require(inbox.orEmpty().any { it.id == id })
        readIds.add(id)
        Unit
    }
    override suspend fun markAllRead() = mutex.withLock {
        readIds.addAll(inbox.orEmpty().map { it.id })
        Unit
    }
    override suspend fun canOpen(notification: FamilyNotification): Boolean {
        if (!notification.targetAccessible) return false
        val family = home.load()
        if (family.browsing) return false
        return when (notification.target) {
            NotificationTarget.QUESTION -> family.question?.id == notification.targetId
            NotificationTarget.SCHEDULE, NotificationTarget.MEMORY -> family.schedules.any { it.id == notification.targetId }
            NotificationTarget.MISSION -> family.mission?.id == notification.targetId
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationRepositoryModule {
    @Provides @Singleton fun provideNotificationRepository(home: HomeRepository): NotificationRepository = FakeNotificationRepository(home)
}
