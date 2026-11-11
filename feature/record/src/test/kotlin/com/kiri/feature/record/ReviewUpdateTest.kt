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
        var deleteResult: Result<Unit> = Result.success(Unit)
        var changeResult: Result<Long>? = null
        val changes = mutableListOf<Long>()
        // 다른 가족이 쓴 리뷰. 사진 두 장 (id 21, 22)
        val other = ScheduleReview(
            id = 4, writer = ReviewWriter(2, "엄마", null), content = "좋았어", starRating = 5, rejoinIntended = true,
            imageUrls = listOf("https://cdn/c.jpg", "https://cdn/d.jpg"), createdAt = null, isMine = false,
            photoIds = mapOf("https://cdn/c.jpg" to 21L, "https://cdn/d.jpg" to 22L),
        )
        var withOther = false
        // 서버가 목록 조회에 함께 주는 대표 사진 id (정하지 않았으면 null)
        var representative: Long? = null
        val deleted = mutableListOf<Long>()
        // 지운 뒤에는 목록에서 빠진다
        private var removed = false
        val mine = ScheduleReview(
            id = 3, writer = ReviewWriter(1, "나", null), content = "즐거웠어요", starRating = 4, rejoinIntended = true,
            imageUrls = listOf("https://cdn/user/original/a.jpg", "https://cdn/user/original/b.jpg"),
            createdAt = null, isMine = true,
            photoIds = mapOf("https://cdn/user/original/a.jpg" to 11L, "https://cdn/user/original/b.jpg" to 12L),
            photoKeys = mapOf("https://cdn/user/original/a.jpg" to "user/original/a.jpg", "https://cdn/user/original/b.jpg" to "user/original/b.jpg"),
        )
        override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> {
            gets++
            return Result.success((if (removed) emptyList() else listOf(mine)) + (if (withOther) listOf(other) else emptyList()))
        }
        override suspend fun getReviewList(scheduleId: Long): Result<com.kiri.domain.model.review.ScheduleReviewList> =
            getReviews(scheduleId).map { com.kiri.domain.model.review.ScheduleReviewList(it, representative) }
        override suspend fun changeRepresentativePhoto(scheduleId: Long, reviewPhotoId: Long): Result<Long> {
            changes += reviewPhotoId
            return changeResult ?: Result.success(reviewPhotoId).also { representative = reviewPhotoId }
        }
        override suspend fun deleteReview(scheduleId: Long, reviewId: Long): Result<Unit> {
            deleted += reviewId
            if (deleteResult.isSuccess) removed = true
            return deleteResult
        }
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

    @Test fun 내_리뷰를_지우면_목록에서_빠지고_다시_기록을_남길_수_있다() = runTest(dispatcher) {
        val reviews = Reviews()
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()
        viewModel.editReview(3)
        viewModel.effect.first()

        viewModel.deleteReview(3)
        advanceUntilIdle()

        assertEquals(listOf(3L), reviews.deleted)
        val state = viewModel.uiState.value
        assertTrue(state.reviews.isEmpty())
        assertFalse(state.reviewSaved)
        assertNull(state.editingReviewId)
        assertEquals("", state.detail.memoryDraft.text)
        assertEquals("기록을 삭제했어요.", state.notice)
        assertFalse(state.detail.saving)
    }

    @Test fun 지우지_못하면_리뷰를_그대로_두고_이유를_알린다() = runTest(dispatcher) {
        val reviews = Reviews().apply { deleteResult = Result.failure(ReviewAccessDeniedException()) }
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()

        viewModel.deleteReview(3)
        advanceUntilIdle()
        // 내 리뷰가 아니면 요청하지 않는다
        viewModel.deleteReview(99)
        advanceUntilIdle()

        assertEquals(listOf(3L), reviews.deleted)
        assertEquals(1, viewModel.uiState.value.reviews.size)
        assertTrue(viewModel.uiState.value.reviewSaved)
        assertEquals("내가 쓴 기록만 삭제할 수 있어요.", viewModel.uiState.value.detail.error)
    }

    @Test fun 대표_사진은_목록_조회의_대표_사진_id이고_없으면_첫_사진이며_바꾸면_반영한다() = runTest(dispatcher) {
        val reviews = Reviews().apply { withOther = true }
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()
        // 서버가 대표 사진을 주지 않으면 리뷰 목록의 첫 사진
        assertEquals("11", viewModel.uiState.value.representativePhoto?.id)

        // 서버가 준 대표 사진 id 를 쓴다 (다른 가족이 바꾼 경우 등)
        reviews.representative = 21
        viewModel.retryReviews()
        advanceUntilIdle()
        assertEquals("21", viewModel.uiState.value.representativePhoto?.id)
        reviews.representative = null
        viewModel.retryReviews()
        advanceUntilIdle()
        assertEquals(listOf("11", "12", "21", "22"), viewModel.uiState.value.reviewPhotos.map { it.id })

        viewModel.openPhotoChange()
        assertEquals(ServerScheduleDetailEffect.OpenPhotoChange, viewModel.effect.first())
        // 지금 대표 사진을 다시 고르면 저장하지 않는다
        viewModel.selectRepresentativePhoto("11")
        viewModel.saveRepresentativePhoto {}
        advanceUntilIdle()
        assertTrue(reviews.changes.isEmpty())

        var closed = false
        viewModel.selectRepresentativePhoto("22")
        viewModel.saveRepresentativePhoto { closed = true }
        advanceUntilIdle()

        assertEquals(listOf(22L), reviews.changes)
        assertTrue(closed)
        // 다시 불러온 목록의 대표 사진 id 로 보여준다
        assertEquals(22L, viewModel.uiState.value.representativePhotoId)
        assertEquals("22", viewModel.uiState.value.representativePhoto?.id)
        assertEquals("https://cdn/d.jpg", viewModel.uiState.value.representativePhoto?.source)
        assertEquals("대표 사진을 변경했어요.", viewModel.uiState.value.notice)
    }

    @Test fun 대표_사진을_바꾸지_못하면_이유를_알리고_사진이_한_장뿐이면_열지_않는다() = runTest(dispatcher) {
        val reviews = Reviews().apply { withOther = true; changeResult = Result.failure(com.kiri.domain.model.review.ReviewPhotoNotFoundException()) }
        val viewModel = vm(Photos(), reviews)
        advanceUntilIdle()
        viewModel.selectRepresentativePhoto("21")
        var closed = false

        viewModel.saveRepresentativePhoto { closed = true }
        advanceUntilIdle()

        assertFalse(closed)
        assertEquals("지워진 사진이에요. 다른 사진을 골라주세요.", viewModel.uiState.value.detail.error)
        assertEquals("11", viewModel.uiState.value.representativePhoto?.id)

        // 고를 사진이 두 장 미만이면(내 리뷰를 지워 사진이 없음) 변경 화면을 열지 않는다
        val empty = vm(Photos(), Reviews().apply { deleteResult = Result.success(Unit) })
        advanceUntilIdle()
        empty.deleteReview(3)
        advanceUntilIdle()
        empty.openPhotoChange()
        assertNull(withTimeoutOrNull(100) { empty.effect.first() })
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
