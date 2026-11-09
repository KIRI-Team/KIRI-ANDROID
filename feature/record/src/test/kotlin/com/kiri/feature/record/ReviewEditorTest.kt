package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.*
import com.kiri.domain.home.*
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.schedule.*
import com.kiri.domain.repository.*
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import com.kiri.feature.detail.model.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewEditorTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC)
    private class Home : HomeRepository {
        override suspend fun load() = FamilyHome(LocalDate.parse("2026-11-08"), "1", "가족",
            listOf(FamilyMember("1", "나", "DAUGHTER"), FamilyMember("2", "엄마", "MOTHER")), null, emptyMap(), emptySet(), null)
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = error("unused")
        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = error("unused")
    }
    private class Schedules : CalendarScheduleRepository {
        var completed = false
        var mine = MyScheduleStatus.ACCEPTED
        var date = LocalDate.parse("2026-11-08")
        var proposer = "엄마"
        var cancelled = false
        var cancelFails = true
        var accepted: Boolean? = null
        var responseFails = false
        override suspend fun getMonthlySchedules(month: YearMonth) = Result.success(listOf(CalendarSchedule(7, date, LocalTime.of(10,0), CalendarScheduleStatus.CONFIRMED, mine)))
        override suspend fun getScheduleDetail(scheduleId: Long) = Result.success(ScheduleDetail(date, LocalTime.of(10,0),
            if (completed) ScheduleDisplayStatus.COMPLETED else if (date == LocalDate.parse("2026-11-08")) ScheduleDisplayStatus.TODAY else ScheduleDisplayStatus.CONFIRMED,
            listOf(ScheduleParticipant(proposer, true), ScheduleParticipant("나", mine == MyScheduleStatus.ACCEPTED))))
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate) = Result.success(Unit)
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate) = Result.success(8L)
        override suspend fun cancel(scheduleId: Long): Result<Unit> {
            if (cancelFails) return Result.failure(Exception("취소 실패"))
            cancelled = true; return Result.success(Unit)
        }
        override suspend fun respond(scheduleId: Long, accept: Boolean): Result<Unit> {
            if (responseFails) return Result.failure(Exception("응답 실패"))
            accepted = accept; mine = if (accept) MyScheduleStatus.ACCEPTED else MyScheduleStatus.DECLINED
            return Result.success(Unit)
        }
    }
    private class Photos : ReviewPhotoRepository {
        var fail: String? = "photo2"
        val uploads = mutableListOf<String>()
        override suspend fun upload(uri: String): Result<String> {
            uploads += uri
            return if (uri == fail) Result.failure(Exception()) else Result.success("key-$uri")
        }
    }
    private class Reviews : ScheduleReviewRepository {
        var fail = true
        var onPosted: () -> Unit = {}
        val keys = mutableListOf<List<String>>()
        override suspend fun getReviews(scheduleId: Long) = Result.success(emptyList<com.kiri.domain.model.review.ScheduleReview>())
        override suspend fun writeReview(scheduleId: Long, form: com.kiri.domain.model.review.ScheduleReviewForm): Result<com.kiri.domain.model.review.ScheduleReviewResult> {
            keys += form.imageKeys
            return if (fail) Result.failure(Exception()) else { onPosted(); Result.success(com.kiri.domain.model.review.ScheduleReviewResult(1, scheduleId)) }
        }
    }
    private fun vm(photos: Photos, reviews: Reviews, saved: SavedStateHandle, schedules: Schedules = Schedules()): ServerScheduleDetailViewModel {
        val home = Home()
        val missions = object : MissionInfoRepository { override suspend fun getMission(missionId: Long): Result<MissionInfo> = Result.failure(Exception()) }
        return ServerScheduleDetailViewModel(saved, GetScheduleEditAccessUseCase(schedules, home), missions,
            reviews = reviews, home = home, schedules = schedules, clock = clock, photos = photos)
    }
    @Test fun partialUploadRetryRestoresDraftAndNeverPostsTwiceAfterSuccess() = runTest(dispatcher) {
        val photos = Photos(); val reviews = Reviews(); val saved = SavedStateHandle(mapOf("scheduleId" to 7L))
        val vm = vm(photos, reviews, saved); advanceUntilIdle()
        val draft = MemoryDraft(5, listOf("photo1", "photo2"), "가족 기록", true)
        vm.updateDraft(draft); vm.submitReview {}; advanceUntilIdle()
        assertEquals(listOf("photo1", "photo2"), photos.uploads); assertTrue(reviews.keys.isEmpty())
        photos.fail = null
        val restored = vm(photos, reviews, saved); advanceUntilIdle()
        assertEquals(draft, restored.uiState.value.detail.memoryDraft)
        restored.submitReview {}; advanceUntilIdle()
        assertEquals(listOf("photo1", "photo2", "photo2"), photos.uploads)
        assertFalse(restored.uiState.value.reviewSaved); assertNotNull(restored.uiState.value.detail.error)
        reviews.fail = false; var completed = 0
        restored.submitReview { completed++ }; advanceUntilIdle()
        restored.submitReview { completed++ }; advanceUntilIdle()
        assertEquals(1, completed); assertEquals(2, reviews.keys.size)
        assertEquals(listOf("key-photo1", "key-photo2"), reviews.keys.last())
        val recreated = vm(photos, reviews, saved); advanceUntilIdle()
        recreated.submitReview { completed++ }; advanceUntilIdle()
        assertEquals(1, completed)
    }
    @Test fun successfulReviewRefreshesServerCompletionAndShowsRecord() = runTest(dispatcher) {
        val schedules = Schedules()
        val reviews = Reviews().apply { fail = false; onPosted = { schedules.completed = true } }
        val photos = Photos().apply { fail = null }
        val vm = vm(photos, reviews, SavedStateHandle(mapOf("scheduleId" to 7L)), schedules)
        advanceUntilIdle()
        vm.updateDraft(MemoryDraft(4, listOf("photo"), "좋은 시간", true))
        vm.submitReview {}; advanceUntilIdle()
        assertTrue(vm.uiState.value.showsRecord)
        assertTrue(vm.uiState.value.reviewSaved)
    }

}
