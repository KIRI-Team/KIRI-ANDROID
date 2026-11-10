package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.home.*
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.review.ReviewAccessDeniedException
import com.kiri.domain.model.review.ReviewWriter
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult
import com.kiri.domain.model.schedule.*
import com.kiri.domain.repository.*
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

// 완료된 일정의 내 리뷰를 수정 화면으로 고쳐 저장하는지 확인한다 (리뷰 수정 API: 전체 교체)
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewUpdateTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val clock = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC)

    private class Home : HomeRepository {
        override suspend fun load() = FamilyHome(LocalDate.parse("2026-11-10"), "1", "가족",
            listOf(FamilyMember("1", "나", "딸"), FamilyMember("2", "엄마", "엄마")), null, emptyMap(), emptySet(), null)
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = error("unused")
        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = error("unused")
    }

    // 지난 날짜의 완료된 일정. 기록 화면(리뷰 목록)으로 보인다
    private class Schedules : CalendarScheduleRepository {
        private val date = LocalDate.parse("2026-11-08")
        override suspend fun getMonthlySchedules(month: YearMonth) =
            Result.success(listOf(CalendarSchedule(7, date, LocalTime.of(10, 0), CalendarScheduleStatus.COMPLETED, MyScheduleStatus.ACCEPTED)))
        override suspend fun getScheduleDetail(scheduleId: Long) = Result.success(ScheduleDetail(date, LocalTime.of(10, 0),
            ScheduleDisplayStatus.COMPLETED, listOf(ScheduleParticipant("엄마", true), ScheduleParticipant("나", true))))
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate) = Result.success(Unit)
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate) = Result.success(8L)
    }

    private class Photos : ReviewPhotoRepository {
        val uploads = mutableListOf<String>()
        override suspend fun upload(uri: String): Result<String> { uploads += uri; return Result.success("pending/$uri") }
    }

    private class Reviews : ScheduleReviewRepository {
        var updateResult: Result<ScheduleReviewResult> = Result.success(ScheduleReviewResult(3, 7))
        val updates = mutableListOf<Pair<Long, ScheduleReviewForm>>()
        var gets = 0
        val mine = ScheduleReview(
            id = 3, writer = ReviewWriter(1, "나", null), content = "즐거웠어요", starRating = 4, rejoinIntended = true,
            imageUrls = listOf("https://cdn/user/original/a.jpg", "https://cdn/user/original/b.jpg"),
            createdAt = null, isMine = true,
            photoKeys = mapOf("https://cdn/user/original/a.jpg" to "user/original/a.jpg", "https://cdn/user/original/b.jpg" to "user/original/b.jpg"),
        )
        override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> { gets++; return Result.success(listOf(mine)) }
        override suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> = error("수정에서는 작성하지 않는다")
        override suspend fun updateReview(scheduleId: Long, reviewId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> {
            updates += reviewId to form
            return updateResult
        }
    }

    private fun vm(photos: Photos, reviews: Reviews, saved: SavedStateHandle = SavedStateHandle(mapOf("scheduleId" to 7L))): ServerScheduleDetailViewModel {
        val home = Home()
        val schedules = Schedules()
        val missions = object : MissionInfoRepository { override suspend fun getMission(missionId: Long): Result<MissionInfo> = Result.failure(Exception()) }
        return ServerScheduleDetailViewModel(saved, GetScheduleEditAccessUseCase(schedules, home), missions,
            reviews = reviews, home = home, schedules = schedules, clock = clock, photos = photos)
    }

    @Test fun 수정하기는_기존_리뷰로_작성_화면을_채워_연다() = runTest(dispatcher) {
        val viewModel = vm(Photos(), Reviews())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reviewSaved)

        viewModel.editReview(3)

        val draft = viewModel.uiState.value.detail.memoryDraft
        assertEquals(4, draft.rating)
        assertEquals("즐거웠어요", draft.text)
        assertEquals(true, draft.repeatActivity)
        assertEquals(listOf("https://cdn/user/original/a.jpg", "https://cdn/user/original/b.jpg"), draft.photos)
        assertEquals(3L, viewModel.uiState.value.editingReviewId)
        assertEquals(ServerScheduleDetailEffect.OpenRecord, viewModel.effect.first())
        // 이미 등록한 뒤라도 수정 중에는 고칠 수 있다
        viewModel.updateDraft(draft.copy(text = "더 즐거웠어요"))
        assertEquals("더 즐거웠어요", viewModel.uiState.value.detail.memoryDraft.text)
    }

    @Test fun 저장하면_남긴_기존_사진_Key와_새_사진_Key를_순서대로_보내고_목록을_다시_불러온다() = runTest(dispatcher) {
        val photos = Photos(); val reviews = Reviews()
        val viewModel = vm(photos, reviews)
        advanceUntilIdle()
        viewModel.editReview(3)
        val draft = viewModel.uiState.value.detail.memoryDraft
        // a 는 지우고, 새 사진을 앞에, b 는 그대로 둔다
        viewModel.updateDraft(draft.copy(rating = 5, text = "다시 갈래요", repeatActivity = false,
            photos = listOf("content://new", "https://cdn/user/original/b.jpg")))
        val getsBefore = reviews.gets
        var saved = false

        viewModel.submitReview { saved = true }
        advanceUntilIdle()

        val (reviewId, form) = reviews.updates.single()
        assertEquals(3L, reviewId)
        assertEquals(listOf("pending/content://new", "user/original/b.jpg"), form.imageKeys)
        assertEquals(5, form.starRating)
        assertEquals("다시 갈래요", form.content)
        assertEquals(false, form.rejoinIntended)
        assertEquals(listOf("content://new"), photos.uploads)
        assertTrue(saved)
        assertNull(viewModel.uiState.value.editingReviewId)
        assertEquals("기록을 수정했어요.", viewModel.uiState.value.notice)
        assertTrue(reviews.gets > getsBefore)
    }

    @Test fun 수정하지_못하면_입력을_두고_이유를_알린다() = runTest(dispatcher) {
        val reviews = Reviews().apply { updateResult = Result.failure(ReviewAccessDeniedException()) }
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()
        viewModel.editReview(3)
        var saved = false

        viewModel.submitReview { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertEquals("내가 쓴 기록만 수정할 수 있어요.", viewModel.uiState.value.detail.error)
        assertEquals(3L, viewModel.uiState.value.editingReviewId)
        assertEquals("즐거웠어요", viewModel.uiState.value.detail.memoryDraft.text)
    }

    @Test fun Key를_모르는_기존_사진은_보내지_않고_다시_고르게_한다() = runTest(dispatcher) {
        val reviews = Reviews()
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()
        viewModel.editReview(3)
        viewModel.updateDraft(viewModel.uiState.value.detail.memoryDraft.copy(photos = listOf("https://cdn/other/c.jpg")))

        viewModel.submitReview {}
        advanceUntilIdle()

        assertTrue(reviews.updates.isEmpty())
        assertEquals("기존 사진 정보를 찾지 못했어요. 사진을 지우고 다시 골라주세요.", viewModel.uiState.value.detail.error)
    }

    @Test fun 수정_중인_상태는_화면이_다시_만들어져도_이어진다() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("scheduleId" to 7L))
        val first = vm(Photos(), Reviews(), saved)
        advanceUntilIdle()
        first.editReview(3)

        val restored = vm(Photos(), Reviews(), saved)

        assertEquals(3L, restored.uiState.value.editingReviewId)
        assertEquals("user/original/a.jpg", restored.uiState.value.editingPhotoKeys["https://cdn/user/original/a.jpg"])
        assertEquals("즐거웠어요", restored.uiState.value.detail.memoryDraft.text)
    }
}
