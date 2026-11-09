package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.Condition
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.detail.DetailParticipant
import com.kiri.domain.detail.DetailSuggestion
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionGuide
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.mission.MissionNotFoundException
import com.kiri.domain.model.mission.MissionPlace
import com.kiri.domain.model.mission.WalkingLevel
import com.kiri.domain.repository.MissionInfoRepository
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.review.ReviewWriter
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult
import com.kiri.domain.repository.ScheduleReviewRepository
import com.kiri.domain.model.condition.ConditionCustom
import com.kiri.domain.model.condition.ConditionCustomUnavailableException
import com.kiri.domain.model.condition.NotFinalParticipantException
import com.kiri.domain.repository.ScheduleConditionRepository
import com.kiri.domain.detail.NearbyPlace
import com.kiri.domain.repository.ScheduleNearbyRepository
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.detail.model.DetailUiEvent
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 일정(missionId 42)의 미션. 명세 예시를 줄였다
private val mission = MissionInfo(
    id = 42,
    title = "국립현대미술관 서울 관람하기",
    description = null,
    category = "EXHIBITION",
    missionType = "PERMANENT",
    status = null,
    indoorOutdoor = "INDOOR",
    imageUrl = null,
    imageSource = null,
    startDate = null,
    endDate = null,
    openingTime = null,
    closingTime = null,
    price = 0,
    durationMinMinutes = 60,
    durationMaxMinutes = 120,
    walkingLevel = WalkingLevel.MEDIUM,
    sourceUrl = null,
    place = MissionPlace(52, "국립현대미술관 서울", "서울특별시 종로구 삼청로 30", null, null),
    prepareItems = listOf(MissionGuide("사전 예약", null)),
    steps = listOf(MissionGuide("상설 전시 관람하기", "마음에 드는 작품 고르기")),
    bucketState = MissionBucketState.NONE,
    averageRating = null,
    reviewCount = 0,
)

// 캘린더에서 연 서버 일정 상세 화면이 일정 상세 API 결과로 채워지는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class ServerScheduleDetailViewModelTest {

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    private class FakeSchedules(var result: Result<ScheduleDetail>) : CalendarScheduleRepository {
        val requested = mutableListOf<Long>()

        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
            Result.success(emptyList())

        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> {
            requested += scheduleId
            return result
        }

        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> = Result.success(Unit)

        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> = Result.success(1)
    }

    private val detail = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = ScheduleDisplayStatus.TODAY,
        participants = listOf(ScheduleParticipant("엄마", true), ScheduleParticipant("첫째", false)),
        missionId = 42,
    )

    private class FakeMissions(var result: Result<MissionInfo> = Result.success(mission)) : MissionInfoRepository {
        val requested = mutableListOf<Long>()

        override suspend fun getMission(missionId: Long): Result<MissionInfo> {
            requested += missionId
            return result
        }
    }

    private fun viewModel(
        repo: CalendarScheduleRepository,
        id: Long? = 7,
        date: String? = null,
        home: HomeRepository = FakeFamily("나"),
        missions: MissionInfoRepository = FakeMissions(),
        reviews: ScheduleReviewRepository = FakeReviews(),
        conditions: ScheduleConditionRepository? = null,
        nearby: ScheduleNearbyRepository = FakeNearby(),
    ) =
        ServerScheduleDetailViewModel(
            SavedStateHandle(
                listOfNotNull(
                    id?.let { ServerScheduleDetailViewModel.SCHEDULE_ID_ARG to it },
                    date?.let { ServerScheduleDetailViewModel.DATE_ARG to it },
                ).toMap(),
            ),
            GetScheduleEditAccessUseCase(repo, home),
            missions,
            reviews,
            home,
            conditions ?: FakeConditions(Result.failure(IOException())),
            nearby,
        )

    // 정해 둔 근처 놀거리를 돌려주고, 요청한 일정 id 를 기록한다
    private class FakeNearby(var result: Result<List<NearbyPlace>> = Result.success(emptyList())) : ScheduleNearbyRepository {
        val requested = mutableListOf<Long>()

        override suspend fun getNearby(scheduleId: Long): Result<List<NearbyPlace>> {
            requested += scheduleId
            return result
        }
    }

    private val places = listOf(
        NearbyPlace("nearby-0-세빛섬", "세빛섬", "서울시 서초구", "일정 장소에서 가까운 곳이에요", costWon = 30000),
        NearbyPlace("nearby-1-반포", "반포한강공원", "서울시 서초구", "잠깐 쉬어가기 좋은 곳이에요", costWon = 0),
    )

    @Test
    fun 완료되지_않은_일정은_근처_놀거리를_불러와_서버_순서대로_상세에_채운다() = runTest {
        val nearby = FakeNearby(Result.success(places))
        val vm = viewModel(FakeSchedules(Result.success(detail)), nearby = nearby)

        assertEquals(listOf(7L), nearby.requested)
        assertEquals(places, vm.uiState.value.nearby)
        // 미션을 늦게 불러와도 근처 놀거리를 지우지 않고, 조건이 있어도 앱에서 다시 정렬하지 않는다
        val screen = vm.uiState.value.screenDetail
        assertEquals(places, screen.detail!!.nearby)
        assertEquals(places, screen.copy(detail = screen.detail!!.copy(conditions = setOf(Condition.LOWER_BUDGET))).nearby)
    }

    @Test
    fun 완료된_일정은_근처_놀거리를_부르지_않고_실패하면_비워_둔다() = runTest {
        val completed = FakeNearby(Result.success(places))
        viewModel(FakeSchedules(Result.success(detail.copy(status = ScheduleDisplayStatus.COMPLETED))), nearby = completed)
        assertTrue(completed.requested.isEmpty())

        val failed = viewModel(FakeSchedules(Result.success(detail)), nearby = FakeNearby(Result.failure(IOException())))
        assertTrue(failed.uiState.value.nearby.isEmpty())
        // 근처 놀거리 실패는 상세 화면 오류로 보지 않는다
        assertNull(failed.uiState.value.detail.error)
    }

    @Test
    fun 컨디션을_적용하면_다시_정렬된_근처_놀거리를_새로_불러온다() = runTest {
        val nearby = FakeNearby(Result.success(places))
        val vm = viewModel(
            FakeSchedules(Result.success(detail)),
            conditions = FakeConditions(Result.success(custom)),
            nearby = nearby,
        )
        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())

        nearby.result = Result.success(places.reversed())
        vm.onEvent(DetailUiEvent.ApplyClicked)

        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
        assertEquals(listOf(7L, 7L), nearby.requested)
        assertEquals(places.reversed(), vm.uiState.value.nearby)
    }

    // 정해 둔 컨디션 커스텀 정보를 돌려주고, 요청한 일정 id 를 기록한다
    private class FakeConditions(
        var result: Result<ConditionCustom>,
        var applyResult: Result<Unit> = Result.success(Unit),
    ) : ScheduleConditionRepository {
        val requested = mutableListOf<Long>()
        val applied = mutableListOf<Pair<Long, Set<Condition>>>()

        override suspend fun getConditionCustom(scheduleId: Long): Result<ConditionCustom> {
            requested += scheduleId
            return result
        }

        override suspend fun applyConditions(scheduleId: Long, conditions: Set<Condition>): Result<Unit> {
            applied += scheduleId to conditions
            return applyResult
        }
    }

    @Test
    fun 적용하기를_누르면_고른_조건을_저장하고_상세에_반영한_뒤_돌아간다() = runTest {
        val conditions = FakeConditions(Result.success(custom))
        val vm = viewModel(FakeSchedules(Result.success(detail)), conditions = conditions)
        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())

        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.AVOID_CROWDS))
        vm.onEvent(DetailUiEvent.ApplyClicked)

        val chosen = setOf(Condition.LESS_WALKING, Condition.AVOID_CROWDS)
        assertEquals(listOf(7L to chosen), conditions.applied)
        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
        assertFalse(vm.uiState.value.detail.saving)
        // 상세의 "컨디션에 맞게 추천 순서를 조정했어요" 는 적용된 조건으로 정한다
        assertEquals(chosen, vm.uiState.value.detail.detail!!.conditions)
    }

    @Test
    fun 모든_조건을_해제해도_빈_조건으로_적용한다() = runTest {
        val conditions = FakeConditions(Result.success(custom))
        val vm = viewModel(FakeSchedules(Result.success(detail)), conditions = conditions)
        vm.onEvent(DetailUiEvent.CustomClicked)
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LESS_WALKING))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))

        vm.onEvent(DetailUiEvent.ApplyClicked)

        assertEquals(listOf(7L to emptySet<Condition>()), conditions.applied)
        assertTrue(vm.uiState.value.detail.detail!!.conditions.isEmpty())
    }

    @Test
    fun 적용하지_못하면_고른_조건을_두고_이유를_보여준다() = runTest {
        listOf(
            ConditionCustomUnavailableException() to "확정된 약속의 당일에만 컨디션을 조정할 수 있어요.",
            NotFinalParticipantException() to "약속에 참여하기로 한 가족만 컨디션을 조정할 수 있어요.",
            IOException() to "적용하지 못했어요. 선택은 유지됐으니 다시 시도해주세요.",
        ).forEach { (error, message) ->
            val conditions = FakeConditions(Result.success(custom), applyResult = Result.failure(error))
            val vm = viewModel(FakeSchedules(Result.success(detail)), conditions = conditions)
            vm.onEvent(DetailUiEvent.CustomClicked)
            assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())

            vm.onEvent(DetailUiEvent.ApplyClicked)

            assertEquals(message, vm.uiState.value.customError)
            assertFalse(vm.uiState.value.detail.saving)
            assertEquals(custom.conditions, vm.uiState.value.detail.draft)
            assertNull(withTimeoutOrNull(100) { vm.effect.first() })
        }
    }

    private val custom = ConditionCustom(
        missionTitle = "한강 뚜벅뚜벅 축제 가보기",
        walkingLevel = WalkingLevel.HIGH,
        durationMinMinutes = 60,
        durationMaxMinutes = 180,
        conditions = setOf(Condition.LESS_WALKING, Condition.MORE_REST),
    )

    @Test
    fun 컨디션_커스텀에_들어가면_지금_적용된_조건과_미션_정보를_불러와_채운다() = runTest {
        val conditions = FakeConditions(Result.success(custom))
        val vm = viewModel(FakeSchedules(Result.success(detail)), conditions = conditions)

        vm.onEvent(DetailUiEvent.CustomClicked)

        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())
        assertEquals(listOf(7L), conditions.requested)
        val state = vm.uiState.value
        assertFalse(state.customLoading)
        assertNull(state.customError)
        assertEquals(setOf(Condition.LESS_WALKING, Condition.MORE_REST), state.detail.draft)
        val filled = state.detail.detail!!
        assertEquals("한강 뚜벅뚜벅 축제 가보기", filled.title)
        assertEquals("많음", filled.stepsLabel)
        assertEquals("1시간 ~ 3시간", filled.durationLabel)

        // 고른 조건을 바꿀 수 있다
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        assertEquals(setOf(Condition.LESS_WALKING), vm.uiState.value.detail.draft)
    }

    @Test
    fun 컨디션_정보를_불러오지_못하면_이유에_맞는_문구를_보여준다() = runTest {
        listOf(
            NotFinalParticipantException() to "약속에 참여하기로 한 가족만 컨디션을 조정할 수 있어요.",
            ConditionCustomUnavailableException() to "확정된 약속의 당일에만 컨디션을 조정할 수 있어요.",
            ScheduleNotFoundException() to "찾을 수 없는 약속이에요.",
            NoFamilyException() to "가족 공간이 없어요.",
            IOException() to "컨디션 정보를 불러오지 못했어요.",
        ).forEach { (error, message) ->
            val vm = viewModel(FakeSchedules(Result.success(detail)), conditions = FakeConditions(Result.failure(error)))

            vm.onEvent(DetailUiEvent.CustomClicked)

            assertEquals(message, vm.uiState.value.customError)
            assertFalse(vm.uiState.value.customLoading)
            assertTrue(vm.uiState.value.detail.draft.isEmpty())
            // 상세 화면 자체의 오류로 보지 않는다
            assertNull(vm.uiState.value.detail.error)
        }
    }

    // 정해 둔 리뷰 목록을 돌려주고, 요청한 일정 id 를 기록한다
    private class FakeReviews(var result: Result<List<ScheduleReview>> = Result.success(emptyList())) : ScheduleReviewRepository {
        val requested = mutableListOf<Long>()

        override suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> {
            requested += scheduleId
            return result
        }
    }

    private val review = ScheduleReview(
        1, ReviewWriter(5, "김주연", FamilyRole.DAUGHTER), "처음 가봤다.", 5, true,
        listOf("https://cdn.example.com/1.jpg"), null, isMine = true,
    )

    @Test
    fun 완료된_일정은_기록_화면으로_보여주고_리뷰와_가족_이름을_불러온다() = runTest {
        val reviews = FakeReviews(Result.success(listOf(review)))
        val vm = viewModel(FakeSchedules(Result.success(detail.copy(status = ScheduleDisplayStatus.COMPLETED))), reviews = reviews)

        val state = vm.uiState.value
        assertTrue(state.showsRecord)
        assertEquals(listOf(7L), reviews.requested)
        assertEquals(listOf(review), state.reviews)
        assertFalse(state.reviewsLoading)
        assertFalse(state.reviewsFailed)
        assertEquals("우리 가족", state.familyName)
        // 완료된 일정은 컨디션 조정도, 변경 화면 이동도 하지 않는다
        assertFalse(state.canCustomize)
        assertNull(withTimeoutOrNull(100) { vm.effect.first() })
    }

    @Test
    fun 완료되지_않은_일정은_리뷰를_불러오지_않는다() = runTest {
        val reviews = FakeReviews()
        listOf(ScheduleDisplayStatus.TODAY, ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.SCHEDULED).forEach { status ->
            val vm = viewModel(FakeSchedules(Result.success(detail.copy(status = status))), reviews = reviews)
            assertFalse(vm.uiState.value.showsRecord)
        }
        assertTrue(reviews.requested.isEmpty())
    }

    @Test
    fun 리뷰를_불러오지_못하면_실패로_두고_다시_시도하면_새로_불러온다() = runTest {
        val reviews = FakeReviews(Result.failure(IOException()))
        val vm = viewModel(
            FakeSchedules(Result.success(detail.copy(status = ScheduleDisplayStatus.COMPLETED))),
            home = FakeFamily(null),
            reviews = reviews,
        )

        assertTrue(vm.uiState.value.reviewsFailed)
        // 가족 정보를 불러오지 못하면 이름은 비워 두고 화면에서 "우리 가족" 으로 쓴다
        assertNull(vm.uiState.value.familyName)

        reviews.result = Result.success(listOf(review))
        vm.retryReviews()

        assertEquals(listOf(7L, 7L), reviews.requested)
        assertFalse(vm.uiState.value.reviewsFailed)
        assertEquals(listOf(review), vm.uiState.value.reviews)
    }

    @Test
    fun 리뷰_작성자의_가족_역할을_기록_카드_문구로_바꾼다() {
        assertEquals("엄마", FamilyRole.MOTHER.label)
        assertEquals("아빠", FamilyRole.FATHER.label)
        assertEquals("딸", FamilyRole.DAUGHTER.label)
        assertEquals("아들", FamilyRole.SON.label)
        assertEquals("", (null as FamilyRole?).label)
    }

    @Test
    fun 당일이_아닌_일정을_제안자가_열면_상세를_먼저_보여주고_수정하기를_눌러야_변경_화면으로_간다() = runTest {
        listOf(ScheduleDisplayStatus.SCHEDULED, ScheduleDisplayStatus.CONFIRMED).forEach { status ->
            val vm = viewModel(FakeSchedules(Result.success(detail.copy(status = status))), home = FakeFamily("엄마"))

            // 열자마자 변경 화면으로 보내지 않는다
            assertNull(withTimeoutOrNull(100) { vm.effect.first() })
            assertFalse(vm.uiState.value.detail.loading)
            assertEquals(detail.copy(status = status), vm.uiState.value.schedule)
            assertTrue(vm.uiState.value.canEdit)

            vm.editClicked()
            assertEquals(ServerScheduleDetailEffect.OpenEdit(7), vm.effect.first())
        }
    }

    @Test
    fun 당일이거나_완료됐거나_제안자가_아니면_수정하기를_보여주지_않고_눌러도_가지_않는다() = runTest {
        listOf(
            detail to FakeFamily("엄마"),
            detail.copy(status = ScheduleDisplayStatus.COMPLETED) to FakeFamily("엄마"),
            detail.copy(status = ScheduleDisplayStatus.CONFIRMED) to FakeFamily("첫째"),
            // 가족 정보를 불러오지 못하면 제안자인지 알 수 없어 수정하기를 숨긴다
            detail.copy(status = ScheduleDisplayStatus.CONFIRMED) to FakeFamily(null),
        ).forEach { (schedule, family) ->
            val vm = viewModel(FakeSchedules(Result.success(schedule)), home = family)

            assertEquals(schedule, vm.uiState.value.schedule)
            assertFalse(vm.uiState.value.canEdit)
            vm.editClicked()
            assertNull(withTimeoutOrNull(100) { vm.effect.first() })
        }
    }

    @Test
    fun 변경하고_돌아오면_바뀐_일정을_다시_불러온다() = runTest {
        val repo = FakeSchedules(Result.success(detail.copy(status = ScheduleDisplayStatus.CONFIRMED)))
        val vm = viewModel(repo, home = FakeFamily("엄마"))

        repo.result = Result.success(detail.copy(status = ScheduleDisplayStatus.SCHEDULED, time = LocalTime.of(16, 0)))
        vm.reload()

        assertEquals(listOf(7L, 7L), repo.requested)
        assertEquals(ScheduleDisplayStatus.SCHEDULED, vm.uiState.value.schedule?.status)
        assertEquals(LocalTime.of(16, 0), vm.uiState.value.schedule?.time)
    }

    @Test
    fun 열면_해당_일정의_상세를_불러와_날짜_시간_참여자를_채운다() {
        val repo = FakeSchedules(Result.success(detail))

        // 미션은 불러오지 못한 경우로 일정 값만 본다
        val state = viewModel(repo, missions = FakeMissions(Result.failure(IOException()))).uiState.value
        val filled = state.detail.detail!!

        assertEquals(listOf(7L), repo.requested)
        assertFalse(state.detail.loading)
        assertEquals(detail, state.schedule)
        assertEquals("11월 17일 약속", filled.title)
        assertEquals(LocalDate.of(2026, 11, 17), filled.date)
        assertEquals("오후 2시 50분", filled.startTime)
        assertEquals(listOf(DetailParticipant("엄마", true), DetailParticipant("첫째", false)), filled.participants)
        // 일정 API 가 주지 않는 정보는 지어내지 않는다
        assertNull(filled.place)
        assertTrue(filled.activities.isEmpty())
        assertTrue(filled.nearby.isEmpty())
    }

    @Test
    fun 일정의_미션을_불러와_제목_장소_걷기량_활동시간_활동_준비물을_채운다() {
        val missions = FakeMissions()

        val filled = viewModel(FakeSchedules(Result.success(detail)), missions = missions).uiState.value.detail.detail!!

        assertEquals(listOf(42L), missions.requested)
        assertEquals("국립현대미술관 서울 관람하기", filled.title)
        assertEquals("국립현대미술관 서울", filled.place)
        assertEquals("보통", filled.stepsLabel)
        assertEquals("1시간 ~ 2시간", filled.durationLabel)
        assertEquals(listOf(DetailSuggestion("상설 전시 관람하기", "마음에 드는 작품 고르기")), filled.activities)
        assertEquals(listOf(DetailSuggestion("사전 예약", "")), filled.essentials)
        // 날짜·시간·참여자는 일정 값 그대로다
        assertEquals("오후 2시 50분", filled.startTime)
        assertEquals(listOf(DetailParticipant("엄마", true), DetailParticipant("첫째", false)), filled.participants)
    }

    @Test
    fun 미션을_불러오지_못하거나_missionId_가_없으면_일정_정보만_보여준다() {
        val failed = FakeMissions(Result.failure(MissionNotFoundException()))
        val withoutMission = FakeMissions()

        val onFailure = viewModel(FakeSchedules(Result.success(detail)), missions = failed).uiState.value
        val noId = viewModel(FakeSchedules(Result.success(detail.copy(missionId = null))), missions = withoutMission).uiState.value

        listOf(onFailure, noId).forEach { state ->
            assertNull(state.detail.error)
            assertEquals("11월 17일 약속", state.detail.detail!!.title)
            assertNull(state.detail.detail!!.place)
            assertTrue(state.detail.detail!!.activities.isEmpty())
        }
        assertTrue(withoutMission.requested.isEmpty())
    }

    @Test
    fun 제안자의_일정도_상세에_미션을_채운다() = runTest {
        val missions = FakeMissions()

        val vm = viewModel(
            FakeSchedules(Result.success(detail.copy(status = ScheduleDisplayStatus.CONFIRMED))),
            home = FakeFamily("엄마"),
            missions = missions,
        )

        assertEquals(listOf(42L), missions.requested)
        assertEquals(mission.title, vm.uiState.value.detail.detail?.title)
    }

    @Test
    fun 시간을_모르면_비워_두고_완료된_일정은_완료로_표시한다() {
        val repo = FakeSchedules(Result.success(detail.copy(time = null, status = ScheduleDisplayStatus.COMPLETED)))

        val filled = viewModel(repo).uiState.value.detail.detail!!

        assertNull(filled.startTime)
        assertTrue(filled.completed)
    }

    @Test
    fun 불러오지_못하면_오류를_보여주고_다시_시도하면_새로_불러온다() {
        val repo = FakeSchedules(Result.failure(IOException()))
        val vm = viewModel(repo)
        assertEquals("약속 정보를 불러오지 못했어요.", vm.uiState.value.detail.error)
        assertNull(vm.uiState.value.detail.detail)

        repo.result = Result.success(detail)
        vm.onEvent(DetailUiEvent.RetryClicked)

        assertNull(vm.uiState.value.detail.error)
        assertEquals(detail, vm.uiState.value.schedule)
        assertEquals(listOf(7L, 7L), repo.requested)
    }

    @Test
    fun 다시_불러와도_결과가_같은_오류는_다시_시도를_막는다() {
        listOf(
            CancelledScheduleException() to "취소된 약속이에요.",
            ScheduleNotFoundException() to "찾을 수 없는 약속이에요.",
            NoFamilyException() to "가족 공간이 없어요.",
        ).forEach { (error, message) ->
            val repo = FakeSchedules(Result.failure(error))
            val vm = viewModel(repo)
            assertEquals(message, vm.uiState.value.detail.error)
            assertFalse(vm.uiState.value.retryable)

            vm.onEvent(DetailUiEvent.RetryClicked)

            assertEquals(listOf(7L), repo.requested)
        }
    }

    @Test
    fun 네트워크_오류는_다시_시도할_수_있다() {
        val vm = viewModel(FakeSchedules(Result.failure(IOException())))

        assertTrue(vm.uiState.value.retryable)
    }

    @Test
    fun 앱바_제목은_받은_날짜_캘린더_날짜_약속_상세_순으로_정한다() {
        // 받기 전·실패해도 캘린더에서 고른 날짜로 보여준다
        val failed = viewModel(FakeSchedules(Result.failure(IOException())), date = "2026-11-17")
        assertEquals("11월 17일", failed.uiState.value.appBarTitle)

        // 받은 상세의 날짜가 우선이다
        val loaded = viewModel(FakeSchedules(Result.success(detail.copy(date = LocalDate.of(2026, 11, 18)))), date = "2026-11-17")
        assertEquals("11월 18일", loaded.uiState.value.appBarTitle)

        // 날짜가 없거나 읽지 못하면 "약속 상세"
        assertEquals("약속 상세", viewModel(FakeSchedules(Result.failure(IOException()))).uiState.value.appBarTitle)
        assertEquals(
            "약속 상세",
            viewModel(FakeSchedules(Result.failure(IOException())), date = "bad").uiState.value.appBarTitle,
        )
    }

    @Test
    fun 일정_id_가_없으면_요청하지_않고_찾을_수_없다고_보여준다() {
        val repo = FakeSchedules(Result.success(detail))

        val state = viewModel(repo, id = null).uiState.value

        assertTrue(repo.requested.isEmpty())
        assertEquals("찾을 수 없는 약속이에요.", state.detail.error)
        assertFalse(state.retryable)
    }

    @Test
    fun 뒤로가기를_누르면_닫기_효과를_보낸다() = runTest {
        val vm = viewModel(FakeSchedules(Result.success(detail)))

        vm.onEvent(DetailUiEvent.BackClicked)

        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
    }

    @Test
    fun 약속_당일이면_컨디션_커스텀_화면을_열고_조건을_고를_수_있다() = runTest {
        val vm = viewModel(FakeSchedules(Result.success(detail)))
        assertTrue(vm.uiState.value.canCustomize)

        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())

        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LESS_WALKING))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        assertEquals(setOf(Condition.LESS_WALKING), vm.uiState.value.detail.draft)

        vm.onEvent(DetailUiEvent.CustomCancelled)
        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())

        // 다시 들어가면 고른 조건을 비우고 시작한다
        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())
        assertTrue(vm.uiState.value.detail.draft.isEmpty())
    }

    @Test
    fun 당일이_아니거나_불러오지_못했으면_컨디션_커스텀을_열지_않는다() = runTest {
        ScheduleDisplayStatus.entries.filter { it != ScheduleDisplayStatus.TODAY }.forEach { status ->
            val vm = viewModel(FakeSchedules(Result.success(detail.copy(status = status))))
            assertFalse(vm.uiState.value.canCustomize)
            vm.onEvent(DetailUiEvent.CustomClicked)
            assertNull(withTimeoutOrNull(100) { vm.effect.first() })
        }
        val failed = viewModel(FakeSchedules(Result.failure(IOException())))
        assertFalse(failed.uiState.value.canCustomize)
        failed.onEvent(DetailUiEvent.CustomClicked)
        assertNull(withTimeoutOrNull(100) { failed.effect.first() })
    }

    @Test
    fun 배지는_캘린더와_같이_당일을_확정으로_보여준다() {
        assertEquals("약속 확정", ScheduleDisplayStatus.TODAY.badgeLabel())
        assertEquals("약속 확정", ScheduleDisplayStatus.CONFIRMED.badgeLabel())
        assertEquals("약속 예정", ScheduleDisplayStatus.SCHEDULED.badgeLabel())
        assertEquals("약속 완료", ScheduleDisplayStatus.COMPLETED.badgeLabel())
        assertEquals("약속", null.badgeLabel())
    }

    @Test
    fun 약속_예정과_완료는_회색_배지_확정과_당일은_초록_배지다() {
        assertTrue(ScheduleDisplayStatus.SCHEDULED.usesNeutralBadge())
        assertTrue(ScheduleDisplayStatus.COMPLETED.usesNeutralBadge())
        assertFalse(ScheduleDisplayStatus.CONFIRMED.usesNeutralBadge())
        assertFalse(ScheduleDisplayStatus.TODAY.usesNeutralBadge())
        assertFalse(null.usesNeutralBadge())
    }
}

// 내 닉네임만 정해 둔 가족 정보. nickname 이 null 이면 불러오기에 실패한다
internal class FakeFamily(private val nickname: String?, private val others: List<Pair<String, String>> = emptyList()) :
    HomeRepository {
    override suspend fun load(): com.kiri.domain.home.FamilyHome {
        val me = nickname ?: throw java.io.IOException()
        return com.kiri.domain.home.FamilyHome(
            date = LocalDate.of(2026, 11, 4),
            currentUserId = "1",
            familyName = "우리 가족",
            members = listOf(com.kiri.domain.home.FamilyMember("1", me, "")) +
                others.map { (id, name) -> com.kiri.domain.home.FamilyMember(id, name, "") },
            question = null,
            answers = emptyMap(),
            pokedMemberIds = emptySet(),
            mission = null,
            serverBacked = true,
        )
    }

    override suspend fun saveAnswer(question: com.kiri.domain.home.DailyQuestion, answer: String) = load()

    override suspend fun poke(question: com.kiri.domain.home.DailyQuestion, memberId: String) = load()
}
