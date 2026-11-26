package com.kiri.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.mission.model.NearbyPlace
import com.kiri.domain.mission.model.MissionBucketState
import com.kiri.domain.mission.model.MissionInfo
import com.kiri.domain.event.repository.AppEventRepository
import com.kiri.domain.mission.repository.MissionInfoRepository
import com.kiri.domain.mission.repository.MissionNearbyRepository
import com.kiri.feature.detail.MissionPreviewViewModel
import com.kiri.feature.detail.model.MissionPreviewUiEffect
import com.kiri.feature.detail.model.MissionPreviewUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 미션 미리보기는 서버 미션 API 로 내용을 채우고, 버킷 저장도 서버에 한다
@OptIn(ExperimentalCoroutinesApi::class)
class MissionPreviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }

    @After fun teardown() { Dispatchers.resetMain() }

    private fun state(missionId: String = "42") = SavedStateHandle(mapOf("missionId" to missionId))

    @Test fun completedHistoryAllowsOneNewSaveAndSavedMissionCannotBeSavedAgain() = runTest(dispatcher) {
        val missions = Missions(MissionBucketState.COMPLETED_ONLY)
        val vm = MissionPreviewViewModel(state(), missions)
        advanceUntilIdle()
        assertEquals("다시 버킷에 저장", vm.uiState.value.saveLabel)

        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals(listOf(42L), missions.saved)
        assertEquals("저장됨", vm.uiState.value.saveLabel)

        // 다시 불러오면 서버가 저장됨으로 알려준다
        vm.retry()
        advanceUntilIdle()
        assertEquals("저장됨", vm.uiState.value.saveLabel)
        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals(listOf(42L), missions.saved)
    }

    // 가족 정보만 돌려주는 홈 저장소. familyName 이 null 이면 둘러보기 사용자다
    private fun homeWith(familyName: String?) = object : com.kiri.domain.home.repository.HomeRepository {
        private val family = com.kiri.domain.home.repository.FamilyHome(java.time.LocalDate.of(2026, 11, 11), "13", familyName,
            emptyList(), null, emptyMap(), emptySet(), null, serverBacked = true)
        override suspend fun load() = family
        override suspend fun saveAnswer(question: com.kiri.domain.home.repository.DailyQuestion, answer: String) = family
        override suspend fun poke(question: com.kiri.domain.home.repository.DailyQuestion, memberId: String) = family
    }

    @Test fun browsingUserCannotSaveBucketAndSeesNotice() = runTest(dispatcher) {
        val missions = Missions()
        val vm = MissionPreviewViewModel(state(), missions, home = homeWith(familyName = null))
        advanceUntilIdle()

        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()

        assertTrue(missions.saved.isEmpty())
        assertFalse(vm.uiState.value.saving)
        assertEquals(MissionPreviewUiEffect.ShowMessage("가족에 참여하면 이용할 수 있어요."), vm.effect.first())
    }

    @Test fun failedResaveKeepsRepeatActionAvailableForRetry() = runTest(dispatcher) {
        val missions = Missions(MissionBucketState.COMPLETED_ONLY).apply { failSave = true }
        val vm = MissionPreviewViewModel(state(), missions)
        advanceUntilIdle()
        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("다시 버킷에 저장", vm.uiState.value.saveLabel)
        assertNotNull(vm.uiState.value.error)

        missions.failSave = false
        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("저장됨", vm.uiState.value.saveLabel)
    }

    @Test fun defaultsToActivitiesAndRestoresReviewTab() = runTest(dispatcher) {
        val savedState = state()
        val missions = Missions()
        val viewModel = MissionPreviewViewModel(savedState, missions)
        advanceUntilIdle()
        assertEquals(listOf(42L), missions.loaded)
        assertFalse(viewModel.uiState.value.reviewsSelected)
        viewModel.selectReviews(true)
        val restored = MissionPreviewViewModel(savedState, missions)
        advanceUntilIdle()
        assertTrue(restored.uiState.value.reviewsSelected)
        restored.selectReviews(false)
        assertFalse(restored.uiState.value.reviewsSelected)
    }

    @Test fun failedLoadCanRetryWithoutLosingSelectedTab() = runTest(dispatcher) {
        val missions = Missions().apply { failLoad = true }
        val viewModel = MissionPreviewViewModel(state(), missions)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        viewModel.selectReviews(true)
        missions.failLoad = false
        viewModel.retry()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.preview)
        assertTrue(viewModel.uiState.value.reviewsSelected)
    }

    @Test fun nonServerMissionIdShowsErrorWithoutCallingServer() = runTest(dispatcher) {
        val missions = Missions()
        val vm = MissionPreviewViewModel(state("place-river"), missions)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertTrue(missions.loaded.isEmpty())
        vm.onEvent(MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertTrue(missions.saved.isEmpty())
    }

    @Test fun recordsOnlyTheFirstSuccessfulServerDetailLoadPerEntry() = runTest(dispatcher) {
        val missions = Missions().apply { failLoad = true }
        val recorded = mutableListOf<Long>()
        val events = object : AppEventRepository {
            override suspend fun recordMissionDetailView(missionId: Long): Result<Unit> {
                recorded += missionId
                return Result.failure(IllegalStateException("events unavailable"))
            }
        }
        val savedState = state()
        val nearbyRequests = mutableListOf<Long>()
        val nearbyPlace = NearbyPlace(id = "nearby", title = "Park", address = "", description = "", missionId = 73L, distance = 150.5)
        val nearby = object : MissionNearbyRepository {
            override suspend fun getMissionNearby(missionId: Long): Result<List<NearbyPlace>> {
                nearbyRequests += missionId
                return Result.success(listOf(nearbyPlace))
            }
        }
        val vm = MissionPreviewViewModel(savedState, missions, appEvents = events, missionNearby = nearby)
        advanceUntilIdle()
        assertTrue(recorded.isEmpty())
        assertTrue(nearbyRequests.isEmpty())

        missions.failLoad = false
        vm.retry()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(42L), nearbyRequests)
        assertEquals(listOf(nearbyPlace), vm.uiState.value.preview?.detail?.nearby)

        vm.retry()
        MissionPreviewViewModel(savedState, missions, appEvents = events)
        advanceUntilIdle()
        assertEquals(listOf(42L), recorded)
    }

    // 불러온 미션 id 와 버킷에 저장한 미션 id 를 남긴다. 저장에 성공하면 그 뒤로는 저장됨으로 돌려준다
    private class Missions(private var bucketState: MissionBucketState = MissionBucketState.NONE) : MissionInfoRepository {
        val loaded = mutableListOf<Long>()
        val saved = mutableListOf<Long>()
        var failLoad = false
        var failSave = false

        override suspend fun getMission(missionId: Long): Result<MissionInfo> {
            loaded += missionId
            if (failLoad) return Result.failure(IllegalStateException("unavailable"))
            return Result.success(mission(missionId, bucketState))
        }

        override suspend fun saveBucket(missionId: Long): Result<Unit> {
            if (failSave) return Result.failure(IllegalStateException("save"))
            saved += missionId
            bucketState = MissionBucketState.SAVED
            return Result.success(Unit)
        }
    }
}

private fun mission(id: Long, bucketState: MissionBucketState) = MissionInfo(
    id = id, title = "미션", description = null, category = null,
    missionType = null, status = null, indoorOutdoor = null, imageUrl = null,
    imageSource = null, startDate = null, endDate = null, openingTime = null,
    closingTime = null, price = null, durationMinMinutes = null, durationMaxMinutes = null,
    walkingLevel = null, sourceUrl = null, place = null,
    steps = emptyList(), bucketState = bucketState, averageRating = null, reviewCount = 0,
)
