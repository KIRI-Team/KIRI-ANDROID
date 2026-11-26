package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.mission.model.DetailParticipant
import com.kiri.domain.mission.model.DetailSuggestion
import com.kiri.domain.mission.model.MissionDetail
import com.kiri.domain.mission.model.MissionGuide
import com.kiri.domain.mission.model.MissionInfo
import com.kiri.domain.home.repository.HomeRepository
import com.kiri.domain.member.model.FamilyRole
import com.kiri.domain.review.model.ScheduleReview
import com.kiri.domain.review.model.ScheduleReviewForm
import com.kiri.domain.review.model.ScheduleReviewResult
import com.kiri.domain.review.repository.ScheduleReviewRepository
import com.kiri.domain.schedule.model.ConditionCustom
import com.kiri.domain.schedule.model.ConditionCustomUnavailableException
import com.kiri.domain.schedule.model.NotFinalParticipantException
import com.kiri.domain.schedule.repository.ScheduleConditionRepository
import com.kiri.domain.mission.model.NearbyPlace
import com.kiri.domain.schedule.repository.ScheduleNearbyRepository
import com.kiri.domain.mission.repository.MissionInfoRepository
import com.kiri.domain.family.model.NoFamilyException
import com.kiri.domain.schedule.model.CancelledScheduleException
import com.kiri.domain.schedule.usecase.GetScheduleEditAccessUseCase
import com.kiri.domain.schedule.model.ScheduleDetail
import com.kiri.domain.schedule.model.MyScheduleStatus
import com.kiri.domain.schedule.model.ScheduleDisplayStatus
import com.kiri.domain.schedule.model.ScheduleNotFoundException
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.model.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 캘린더에서 고른 서버 일정의 상세 화면 상태
data class ServerScheduleDetailUiState(
    val detail: DetailUiState = DetailUiState(),
    // 받은 상세. 화면 제목·배지를 정하는 데 쓴다
    val schedule: ScheduleDetail? = null,
    // 캘린더에서 넘겨받은 날짜. 상세를 받기 전·받지 못했을 때 앱바 제목에 쓴다
    val requestedDate: LocalDate? = null,
    // 취소·없음·가족 없음처럼 다시 불러와도 결과가 같은 오류면 false
    val retryable: Boolean = true,
    // 상세 API 의 canRespond·canCancel. 서버가 제안자 여부·일정 상태·응답 마감·시작 여부까지 판단한다
    val canRespond: Boolean = false,
    val canCancel: Boolean = false,
    val canReview: Boolean = false,
    // 컨디션 조정하러가기를 보여주고 열 수 있는지. 서버 값이 없어 당일 일정이고, 내가 수락했고, 아직 시작 전인지로 정한다
    val canAdjustConditions: Boolean = false,
    val reviewSaved: Boolean = false,
    // 수정 중인 내 리뷰 id (새로 쓰는 중이면 null). 작성 화면을 수정 모드로 쓴다
    val editingReviewId: Long? = null,
    // 수정 중인 리뷰의 기존 사진 주소 → 사진 Key. 그대로 둔 사진은 이 Key 로 보낸다
    val editingPhotoKeys: Map<String, String> = emptyMap(),
    // 리뷰 목록 조회가 준 일정의 대표 사진 id. 없으면 리뷰 목록의 첫 사진이 대표 사진이다
    val representativePhotoId: Long? = null,
    // 대표 사진 변경 화면에서 고른 사진 id
    val selectedPhotoId: String? = null,
    val myResponse: String? = null,
    val author: String = "나",
    val notice: String? = null,
    // 완료된 일정의 가족 기록(리뷰). 작성순이다
    val reviews: List<ScheduleReview> = emptyList(),
    val reviewsLoading: Boolean = false,
    // 리뷰를 불러오지 못했다. 기록 모아보기에 다시 시도를 보여준다
    val reviewsFailed: Boolean = false,
    // 기록 화면의 "OO 기록", "OO님의 기록 모아보기" 에 쓴다. 불러오지 못하면 null
    val familyName: String? = null,
    // 상세 API 의 canEdit. 일정 변경하기 버튼을 보여준다
    val canEdit: Boolean = false,
    // 컨디션 커스텀 화면 정보를 불러오는 중이다
    val customLoading: Boolean = false,
    val customLoaded: Boolean = false,
    // 컨디션 커스텀 화면 정보를 불러오지 못했을 때 그 화면에 보여줄 문구
    val customError: String? = null,
    // 근처 놀거리 추천 API 결과. 서버가 정렬한 순서 그대로다 (불러오지 못하면 비어 있다)
    val nearby: List<NearbyPlace> = emptyList(),
) {
    // 상세 화면에 그릴 상태. 미션 정보와 따로 불러온 근처 놀거리를 합치고, 서버 순서를 유지한다
    val screenDetail: DetailUiState
        get() = detail.copy(
            detail = detail.detail?.copy(nearby = nearby),
            nearbyRankedByServer = true,
        )

    // 완료된 일정은 미션 상세 대신 기록 화면(Figma 7903:1926)으로 보여준다
    val showsRecord get() = schedule?.status == ScheduleDisplayStatus.COMPLETED

    // 받은 상세의 날짜를 먼저 쓰고, 없으면 캘린더에서 고른 날짜, 그것도 없으면 "약속 상세"
    val appBarTitle
        get() = (schedule?.date ?: requestedDate)?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "약속 상세"

    // 약속 당일인지. 홈에서 연 당일 일정은 가족 기록을 함께 보여준다 (컨디션 조정 버튼은 canAdjustConditions 를 따른다)
    val canCustomize get() = schedule?.status == ScheduleDisplayStatus.TODAY

    // 대표 사진 후보: 리뷰 사진 중 id 가 있는 것 (리뷰 작성순, 리뷰 안 사진 순서)
    val reviewPhotos: List<com.kiri.domain.review.model.RecordPhoto>
        get() = reviews.flatMap { review ->
            review.imageUrls.mapNotNull { url -> review.photoIds[url]?.let { com.kiri.domain.review.model.RecordPhoto(it.toString(), url) } }
        }

    // 대표 사진: 리뷰 목록 조회의 representativePhotoId 사진. 없거나 그 사진을 찾지 못하면 리뷰 목록의 첫 사진
    val representativePhoto: com.kiri.domain.review.model.RecordPhoto?
        get() = reviewPhotos.firstOrNull { it.id == representativePhotoId?.toString() }
            ?: reviews.firstNotNullOfOrNull { review ->
                review.imageUrls.firstOrNull()?.let { url ->
                    com.kiri.domain.review.model.RecordPhoto(review.photoIds[url]?.toString() ?: "review-${review.id}-0", url)
                }
            }
}

sealed interface ServerScheduleDetailEffect {
    data object Back : ServerScheduleDetailEffect

    // 약속 당일에만 컨디션 커스텀 화면을 연다
    data object OpenCustom : ServerScheduleDetailEffect
    data object OpenRecord : ServerScheduleDetailEffect

    // 제안자가 상세의 수정하기를 눌러 일정 변경 화면으로 간다
    data class OpenEdit(val scheduleId: Long) : ServerScheduleDetailEffect

    // 기록 화면의 대표 사진에서 사진 변경을 눌렀다
    data object OpenPhotoChange : ServerScheduleDetailEffect
}

// 일정 상세 API 로 받은 날짜·시간·상태·참여자로 머리말을 먼저 채우고,
// 일정의 missionId 로 미션 상세 API 를 불러와 제목·장소·걷기량·활동시간·필수 활동·준비물을 채운다.
// 미션을 불러오지 못하면 머리말만 보여주고 나머지는 화면의 "미정·정보 없음" 문구로 둔다
@HiltViewModel
class ServerScheduleDetailViewModel
@Inject
constructor(
    private val savedState: SavedStateHandle,
    private val editAccess: GetScheduleEditAccessUseCase,
    private val missions: MissionInfoRepository,
    private val reviews: ScheduleReviewRepository = NoReviews,
    private val home: HomeRepository? = null,
    private val conditions: ScheduleConditionRepository = NoConditions,
    private val nearbyPlaces: ScheduleNearbyRepository = NoNearby,
    private val schedules: com.kiri.domain.schedule.repository.CalendarScheduleRepository? = null,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val photos: com.kiri.domain.review.repository.ReviewPhotoRepository? = null,
) : ViewModel() {
    private var customJob: Job? = null
    private var nearbyJob: Job? = null
    private var loadJob: Job? = null
    private var reviewsJob: Job? = null
    private val scheduleId = savedState.get<Long>(SCHEDULE_ID_ARG)
    private val fromHome = savedState.get<Boolean>("fromHome") == true
    private val state = MutableStateFlow(
        ServerScheduleDetailUiState(
            reviewSaved = savedState.get<Boolean>("reviewPosted") == true,
            editingReviewId = savedState.get<Long>(EDITING_REVIEW_ID),
            editingPhotoKeys = savedState.get<ArrayList<String>>(EDITING_PHOTO_URLS).orEmpty()
                .zip(savedState.get<ArrayList<String>>(EDITING_PHOTO_KEYS).orEmpty()).toMap(),
            detail = DetailUiState(memoryDraft = com.kiri.feature.detail.model.MemoryDraft(
                rating = savedState["reviewRating"] ?: 0,
                photos = savedState.get<ArrayList<String>>("reviewPhotos").orEmpty(),
                text = savedState["reviewText"] ?: "",
                repeatActivity = savedState["reviewRejoin"],
            )),
            requestedDate = savedState.get<String>(DATE_ARG)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        ),
    )
    val uiState = state.asStateFlow()
    private val effects = Channel<ServerScheduleDetailEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.BackClicked -> effects.trySend(ServerScheduleDetailEffect.Back)
            DetailUiEvent.RetryClicked -> if (!uiState.value.detail.loading && uiState.value.retryable) load()
            is DetailUiEvent.MemoriesSelected ->
                state.update { it.copy(detail = it.detail.copy(memoriesSelected = event.selected)) }
            DetailUiEvent.CustomClicked ->
                if (uiState.value.canAdjustConditions) {
                    // 들어갈 때마다 고른 조건을 비우고, 지금 일정에 적용된 조건을 불러와 채운다
                    state.update { it.copy(customError = null, detail = it.detail.copy(draft = emptySet())) }
                    effects.trySend(ServerScheduleDetailEffect.OpenCustom)
                    loadConditionCustom()
                }
            is DetailUiEvent.ConditionToggled ->
                if (uiState.value.customLoaded && !uiState.value.customLoading && !uiState.value.detail.saving) state.update {
                    val draft = it.detail.draft
                    it.copy(
                        detail = it.detail.copy(
                            draft = if (event.condition in draft) draft - event.condition else draft + event.condition,
                        ),
                    )
                }
            DetailUiEvent.CustomCancelled -> effects.trySend(ServerScheduleDetailEffect.Back)
            DetailUiEvent.ApplyClicked -> applyConditions()
            DetailUiEvent.RecordClicked -> openRecord()
        }
    }

    private fun load() {
        val id = scheduleId
        if (id == null) {
            state.update {
                it.copy(retryable = false, detail = it.detail.copy(loading = false, error = "찾을 수 없는 약속이에요."))
            }
            return
        }
        state.update { it.copy(detail = it.detail.copy(loading = true, error = null)) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            editAccess(id)
                .onSuccess { access ->
                    // 일정 변경하기는 서버가 판단한 canEdit 일 때만 보여준다
                    val schedule = access.detail
                    state.update {
                        it.copy(
                            schedule = schedule,
                            canEdit = access.canEdit,
                            detail = it.detail.copy(loading = false, detail = schedule.toMissionDetail(id)),
                        )
                    }
                    loadActions(schedule, access)
                    if (schedule.status == ScheduleDisplayStatus.COMPLETED || (fromHome && schedule.status == ScheduleDisplayStatus.TODAY)) {
                        loadReviews(id)
                        loadFamilyName()
                    }
                    if (schedule.status != ScheduleDisplayStatus.COMPLETED) {
                        loadNearby(id)
                    }
                    val missionId = schedule.missionId ?: return@onSuccess
                    missions.getMission(missionId).onSuccess { mission ->
                        state.update {
                            it.copy(detail = it.detail.copy(detail = schedule.toMissionDetail(id, mission)))
                        }
                    }
                    val essentials = schedules?.getScheduleEssentials(id) ?: Result.success(emptyList())
                    essentials.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                    state.update {
                        it.copy(detail = it.detail.copy(
                            detail = it.detail.detail?.copy(essentials = essentials.getOrNull().orEmpty().map { guide -> guide.toSuggestion() }),
                            essentialsFailed = essentials.isFailure,
                        ))
                    }
                }
                .onFailure { error ->
                    state.update {
                        it.copy(
                            retryable = error.isRetryable(),
                            detail = it.detail.copy(loading = false, error = error.message()),
                        )
                    }
                }
        }
    }

    // 근처 놀거리 조회 실패와 결과 없음은 화면에서 구분한다.
    private fun loadNearby(scheduleId: Long) {
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            nearbyPlaces.getNearby(scheduleId)
                .onSuccess { places -> state.update { it.copy(nearby = places, detail = it.detail.copy(nearbyFailed = false)) } }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    state.update { it.copy(nearby = emptyList(), detail = it.detail.copy(nearbyFailed = true)) }
                }
        }
    }

    // 컨디션 커스텀 화면의 미션 정보(제목·걷기량·활동시간)와 지금 적용된 조건을 불러온다
    // 불러오는 동안 사용자가 조건을 고르지 않았을 때만 적용된 조건으로 채운다
    private fun loadConditionCustom() {
        val id = scheduleId ?: return
        customJob?.cancel()
        state.update { it.copy(customLoading = true, customLoaded = false) }
        customJob = viewModelScope.launch {
            conditions.getConditionCustom(id)
                .onSuccess { custom ->
                    state.update {
                        val current = it.detail
                        it.copy(
                            customLoading = false,
                            customLoaded = true,
                            detail = current.copy(
                                detail = current.detail?.withConditionCustom(custom),
                                draft = if (current.draft.isEmpty()) custom.conditions else current.draft,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    state.update { it.copy(customLoading = false, customError = error.customMessage()) }
                }
        }
    }

    // 고른 조건을 일정에 저장한다. 성공하면 상세에 반영하고("컨디션에 맞게 추천 순서를 조정했어요") 상세로 돌아간다
    // 실패하면 고른 조건은 그대로 두고 컨디션 화면에 이유를 보여준다
    private fun applyConditions() {
        val id = scheduleId ?: return
        val current = uiState.value
        if (!current.canAdjustConditions || current.detail.saving || current.customLoading || !current.customLoaded) return
        val draft = current.detail.draft
        state.update { it.copy(customError = null, detail = it.detail.copy(saving = true)) }
        viewModelScope.launch {
            conditions.applyConditions(id, draft)
                .onSuccess {
                    state.update {
                        it.copy(
                            detail = it.detail.copy(
                                saving = false,
                                detail = it.detail.detail?.copy(conditions = draft),
                            ),
                        )
                    }
                    // 저장한 조건으로 서버가 근처 놀거리를 다시 정렬하므로 새로 불러온다
                    loadNearby(id)
                    effects.send(ServerScheduleDetailEffect.Back)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    state.update {
                        it.copy(customError = error.applyMessage(), detail = it.detail.copy(saving = false))
                    }
                }
        }
    }

    private fun Throwable.applyMessage() = when (this) {
        is NotFinalParticipantException, is ConditionCustomUnavailableException,
        is ScheduleNotFoundException, is NoFamilyException -> customMessage()
        else -> "적용하지 못했어요. 선택은 유지됐으니 다시 시도해주세요."
    }

    private fun Throwable.customMessage() = when (this) {
        is NotFinalParticipantException -> "약속에 참여하기로 한 가족만 컨디션을 조정할 수 있어요."
        is ConditionCustomUnavailableException -> "확정된 약속의 당일에만 컨디션을 조정할 수 있어요."
        is ScheduleNotFoundException -> "찾을 수 없는 약속이에요."
        is NoFamilyException -> "가족 공간이 없어요."
        else -> "컨디션 정보를 불러오지 못했어요."
    }

    // 상세의 수정하기를 눌렀다. 바꿀 수 있는 내 일정일 때만 변경 화면으로 간다
    fun editClicked() {
        val id = scheduleId ?: return
        if (uiState.value.canEdit) effects.trySend(ServerScheduleDetailEffect.OpenEdit(id))
    }

    // 일정 변경 화면에서 저장하고 돌아왔다. 바뀐 날짜·시간·참여자를 다시 불러온다
    fun reload() {
        if (!uiState.value.detail.loading) load()
    }

    // 일정 변경 화면에서 저장하고 돌아왔다. 변경 완료를 알리고(Handoff 9898:21827) 바뀐 일정을 다시 불러온다
    fun edited() {
        state.update { it.copy(notice = "일정 변경이 되었습니다!") }
        reload()
    }

    // 기록 모아보기에서 다시 시도를 눌렀다
    fun retryReviews() {
        val id = scheduleId ?: return
        if ((uiState.value.showsRecord || (fromHome && uiState.value.canCustomize)) && !uiState.value.reviewsLoading) loadReviews(id)
    }

    // 완료된 일정의 가족 기록(리뷰)을 불러온다
    private fun loadReviews(scheduleId: Long) {
        reviewsJob?.cancel()
        state.update { it.copy(reviewsLoading = true, reviewsFailed = false) }
        reviewsJob = viewModelScope.launch {
            reviews.getReviewList(scheduleId)
                .onSuccess { result ->
                    val list = result.reviews
                    state.update {
                        it.copy(reviews = list, representativePhotoId = result.representativePhotoId, reviewsLoading = false,
                            reviewSaved = it.reviewSaved || list.any { review -> review.isMine })
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    state.update { it.copy(reviewsLoading = false, reviewsFailed = true) }
                }
        }
    }

    // 가족 이름은 기록 화면 제목에만 쓰므로 불러오지 못하면 "우리 가족" 으로 둔다
    private fun loadFamilyName() {
        val source = home ?: return
        viewModelScope.launch {
            val name = try {
                source.family().familyName
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            state.update { it.copy(familyName = name) }
        }
    }

    // 수락·거절, 일정 취소 버튼과 내 응답 상태는 상세 API 가 준 값(canRespond·canCancel·myStatus)을 그대로 쓴다.
    // 기록 작성 가능 여부는 서버 값이 없어, 내가 수락했고 시작 시각이 지난 일정인지로 정한다
    private suspend fun loadActions(detail: ScheduleDetail, access: com.kiri.domain.schedule.usecase.ScheduleEditAccess) {
        val family = access.family ?: try { home?.family() } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        val me = family?.members?.find { it.id == family.currentUserId }
        val start = detail.time?.let { detail.date.atTime(it).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant() }
        val status = detail.myStatus
        state.update { it.copy(
            author = me?.nickname ?: "나",
            canRespond = detail.canRespond,
            canCancel = detail.canCancel,
            canReview = status == MyScheduleStatus.ACCEPTED && start != null && !start.isAfter(clock.instant()) &&
                detail.status in setOf(ScheduleDisplayStatus.TODAY, ScheduleDisplayStatus.COMPLETED, ScheduleDisplayStatus.CONFIRMED),
            // 시작 시각을 모르면 당일인 동안은 열어 둔다 (서버도 당일·최종 참여자만 확인한다)
            canAdjustConditions = status == MyScheduleStatus.ACCEPTED && detail.status == ScheduleDisplayStatus.TODAY &&
                (start == null || start.isAfter(clock.instant())),
            myResponse = when (status) {
                MyScheduleStatus.PENDING -> "미응답"
                MyScheduleStatus.ACCEPTED -> "수락"
                MyScheduleStatus.DECLINED -> "거절"
                MyScheduleStatus.RECONFIRM_REQUIRED -> "재확인 필요"
                MyScheduleStatus.NOT_INVITED -> "초대받지 않은 가족 일정"
                null -> null
            }) }
    }

    // leave: 응답 화면에서 응답했다. 성공하면 상세 대신 이전 화면(홈·기록 탭)으로 돌아간다
    fun respond(accept: Boolean, leave: Boolean = false) = mutateAction {
        check(state.value.canRespond) { "응답할 수 있는 약속인지 다시 확인해주세요." }
        requireNotNull(schedules).respond(requireNotNull(scheduleId), accept).getOrThrow()
        if (leave) {
            effects.send(ServerScheduleDetailEffect.Back)
        } else {
            state.update { it.copy(notice = if (accept) "수락했어요." else "거절했어요.") }
            load()
        }
    }
    // 상세 화면이 작업 결과 안내·오류를 토스트로 띄운 뒤 지운다.
    // 상세를 불러오지 못한 오류는 화면 안에 다시 시도와 함께 보여주므로 남겨 둔다
    fun messageShown() {
        state.update {
            it.copy(notice = null, detail = if (it.detail.detail != null) it.detail.copy(error = null) else it.detail)
        }
    }
    fun cancel() = mutateAction {
        // 일정 취소하기 버튼은 서버가 판단한 canCancel 일 때만 보인다
        check(state.value.canCancel) { "취소할 수 있는 약속인지 다시 확인해주세요." }
        requireNotNull(schedules).cancel(requireNotNull(scheduleId)).getOrThrow()
        effects.send(ServerScheduleDetailEffect.Back)
    }
    private fun mutateAction(block: suspend () -> Unit) {
        if (state.value.detail.saving) return
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null)) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(detail = it.detail.copy(error = "처리하지 못했어요. 다시 시도해주세요.")) } }
            finally { state.update { it.copy(detail = it.detail.copy(saving = false)) } }
        }
    }

    fun openRecord() {
        val current = state.value
        if (current.canReview && !current.reviewSaved && current.reviews.none { it.isMine })
            effects.trySend(ServerScheduleDetailEffect.OpenRecord)
    }

    // 내 리뷰의 수정하기: 작성 화면을 기존 별점·사진·내용·재참여 의사로 채워 수정 모드로 연다
    fun editReview(reviewId: Long) {
        val current = state.value
        if (current.detail.saving) return
        val review = current.reviews.firstOrNull { it.id == reviewId && it.isMine } ?: return
        val draft = com.kiri.feature.detail.model.MemoryDraft(review.starRating, review.imageUrls, review.content, review.rejoinIntended)
        savedState[EDITING_REVIEW_ID] = reviewId
        savedState[EDITING_PHOTO_URLS] = ArrayList(review.photoKeys.keys)
        savedState[EDITING_PHOTO_KEYS] = ArrayList(review.photoKeys.values)
        saveDraft(draft)
        state.update {
            it.copy(editingReviewId = reviewId, editingPhotoKeys = review.photoKeys,
                detail = it.detail.copy(memoryDraft = draft, error = null))
        }
        effects.trySend(ServerScheduleDetailEffect.OpenRecord)
    }

    private fun saveDraft(draft: com.kiri.feature.detail.model.MemoryDraft) {
        savedState["reviewRating"] = draft.rating
        savedState["reviewPhotos"] = ArrayList(draft.photos)
        savedState["reviewText"] = draft.text
        savedState["reviewRejoin"] = draft.repeatActivity
    }

    fun updateDraft(draft: com.kiri.feature.detail.model.MemoryDraft) {
        // 리뷰를 등록한 뒤에는 수정 중일 때만 바꿀 수 있다
        if (state.value.detail.saving || (state.value.reviewSaved && state.value.editingReviewId == null)) return
        saveDraft(draft)
        state.update { it.copy(detail = it.detail.copy(memoryDraft = draft, error = null)) }
    }

    fun submitReview(onSaved: () -> Unit) {
        val current = state.value
        current.editingReviewId?.let { reviewId -> return submitEdit(reviewId, onSaved) }
        if (!current.canReview || current.reviewSaved || current.detail.saving || !current.detail.memoryDraft.canSubmit) return
        val draft = current.detail.memoryDraft
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null)) }
        viewModelScope.launch {
            try {
                val keys = draft.photos.mapIndexed { index, uri -> uploadReviewPhoto(uri, index) }
                val id = requireNotNull(scheduleId)
                val result = reviews.writeReview(id, ScheduleReviewForm(draft.rating, draft.text, requireNotNull(draft.repeatActivity), keys))
                if (result.isFailure) {
                    val cause = result.exceptionOrNull()
                    if (cause is CancellationException) throw cause
                    val stored = reviews.getReviews(id).getOrNull()
                    if (cause !is com.kiri.domain.review.model.AlreadyReviewedException && stored?.any { it.isMine } != true) throw requireNotNull(cause)
                }
                savedState["reviewPosted"] = true
                state.update {
                    it.copy(
                        reviewSaved = true,
                        notice = "기록을 등록했어요.",
                        detail = it.detail.copy(memoriesSelected = fromHome || it.detail.memoriesSelected),
                    )
                }
                // 리뷰 등록과 함께 서버 일정이 완료되므로 상태·기록 화면도 다시 조회한다.
                load()
                onSaved()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = if (e is ReviewPhotoUploadException) e.message else "기록을 등록하지 못했어요. 입력은 유지됐으니 다시 시도해주세요."
                state.update { it.copy(detail = it.detail.copy(error = message)) }
            } finally { state.update { it.copy(detail = it.detail.copy(saving = false)) } }
        }
    }

    // 수정 화면의 최종 상태로 리뷰를 바꾼다. 그대로 둔 기존 사진은 저장된 Key 로, 새 사진은 올린 뒤 받은 Key 로 보낸다
    private fun submitEdit(reviewId: Long, onSaved: () -> Unit) {
        val current = state.value
        if (current.detail.saving || !current.detail.memoryDraft.canSubmit) return
        val draft = current.detail.memoryDraft
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null)) }
        viewModelScope.launch {
            try {
                val keys = draft.photos.mapIndexed { index, photo ->
                    current.editingPhotoKeys[photo] ?: run {
                        // 서버 주소인데 Key 를 모르면 그대로 보낼 수 없다 (다시 고르게 한다)
                        if (photo.startsWith("http://") || photo.startsWith("https://")) throw MissingPhotoKeyException()
                        uploadReviewPhoto(photo, index)
                    }
                }
                val id = requireNotNull(scheduleId)
                reviews.updateReview(id, reviewId, ScheduleReviewForm(draft.rating, draft.text, requireNotNull(draft.repeatActivity), keys)).getOrThrow()
                listOf(EDITING_REVIEW_ID, EDITING_PHOTO_URLS, EDITING_PHOTO_KEYS).forEach { savedState.remove<Any>(it) }
                state.update { it.copy(editingReviewId = null, editingPhotoKeys = emptyMap(), notice = "기록을 수정했어요.") }
                loadReviews(id)
                onSaved()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = when (e) {
                    is ReviewPhotoUploadException -> e.message
                    is MissingPhotoKeyException -> "기존 사진 정보를 찾지 못했어요. 사진을 지우고 다시 골라주세요."
                    is com.kiri.domain.review.model.ReviewNotFoundException -> "기록을 찾을 수 없어요. 화면을 새로고침해주세요."
                    is com.kiri.domain.review.model.ReviewAccessDeniedException -> "내가 쓴 기록만 수정할 수 있어요."
                    is com.kiri.domain.review.model.InvalidReviewException -> e.message ?: "입력한 내용을 다시 확인해주세요."
                    else -> "기록을 수정하지 못했어요. 입력은 유지됐으니 다시 시도해주세요."
                }
                state.update { it.copy(detail = it.detail.copy(error = message)) }
            } finally { state.update { it.copy(detail = it.detail.copy(saving = false)) } }
        }
    }

    private suspend fun uploadReviewPhoto(uri: String, index: Int): String {
        val checkpoint = "uploaded:$uri"
        savedState.get<String>(checkpoint)?.let { return it }
        return try {
            requireNotNull(photos).upload(uri).getOrThrow().also { savedState[checkpoint] = it }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw ReviewPhotoUploadException(index + 1, error)
        }
    }

    private class ReviewPhotoUploadException(position: Int, cause: Throwable) :
        Exception("$position" + "번째 사진을 올리지 못했어요. 다시 시도해주세요.", cause)

    private class MissingPhotoKeyException : Exception()

    // 대표 사진 변경 화면을 연다. 고를 사진이 두 장 이상일 때만 연다
    fun openPhotoChange() {
        val current = state.value
        if (current.detail.saving || current.reviewPhotos.size < 2) return
        state.update { it.copy(selectedPhotoId = null, detail = it.detail.copy(error = null)) }
        effects.trySend(ServerScheduleDetailEffect.OpenPhotoChange)
    }

    fun selectRepresentativePhoto(photoId: String) {
        if (state.value.detail.saving) return
        state.update { it.copy(selectedPhotoId = photoId) }
    }

    // 고른 사진을 대표 사진으로 정한다. 가족 모두에게 반영된다
    fun saveRepresentativePhoto(onSaved: () -> Unit) {
        val current = state.value
        val selected = current.selectedPhotoId?.toLongOrNull() ?: return
        if (current.detail.saving || current.selectedPhotoId == current.representativePhoto?.id) return
        val id = scheduleId ?: return
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null), notice = null) }
        viewModelScope.launch {
            reviews.changeRepresentativePhoto(id, selected)
                .onSuccess { photoId ->
                    // 응답의 대표 사진 id 로 바로 바꾸고, 목록도 다시 불러와 서버 값과 맞춘다
                    state.update { it.copy(representativePhotoId = photoId, selectedPhotoId = null, notice = "대표 사진을 변경했어요.") }
                    loadReviews(id)
                    onSaved()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    val message = when (error) {
                        is com.kiri.domain.review.model.ReviewPhotoNotFoundException -> "지워진 사진이에요. 다른 사진을 골라주세요."
                        is com.kiri.domain.review.model.ReviewAccessDeniedException -> "우리 가족의 일정만 바꿀 수 있어요."
                        else -> "대표 사진을 바꾸지 못했어요. 잠시 후 다시 시도해주세요."
                    }
                    state.update { it.copy(detail = it.detail.copy(error = message)) }
                    // 사진이 이미 빠졌으면 목록을 다시 불러와 후보를 맞춘다
                    if (error is com.kiri.domain.review.model.ReviewPhotoNotFoundException) loadReviews(id)
                }
            state.update { it.copy(detail = it.detail.copy(saving = false)) }
        }
    }

    // 내 리뷰를 지운다 (화면에서 한 번 더 확인한 뒤 부른다). 지우면 다시 기록을 남길 수 있다. 일정은 완료 상태로 남는다
    fun deleteReview(reviewId: Long) {
        val current = state.value
        if (current.detail.saving || current.reviews.none { it.id == reviewId && it.isMine }) return
        val id = scheduleId ?: return
        state.update { it.copy(detail = it.detail.copy(saving = true, error = null), notice = null) }
        viewModelScope.launch {
            reviews.deleteReview(id, reviewId)
                .onSuccess {
                    // 등록·수정하던 작성 내용은 지운 리뷰의 것이므로 비운다
                    listOf("reviewPosted", "reviewRating", "reviewPhotos", "reviewText", "reviewRejoin",
                        EDITING_REVIEW_ID, EDITING_PHOTO_URLS, EDITING_PHOTO_KEYS).forEach { savedState.remove<Any>(it) }
                    state.update {
                        it.copy(reviewSaved = false, editingReviewId = null, editingPhotoKeys = emptyMap(), notice = "기록을 삭제했어요.",
                            reviews = it.reviews.filterNot { review -> review.id == reviewId },
                            detail = it.detail.copy(memoryDraft = com.kiri.feature.detail.model.MemoryDraft()))
                    }
                    loadReviews(id)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    val message = when (error) {
                        is com.kiri.domain.review.model.ReviewNotFoundException -> "이미 지워졌거나 찾을 수 없는 기록이에요."
                        is com.kiri.domain.review.model.ReviewAccessDeniedException -> "내가 쓴 기록만 삭제할 수 있어요."
                        else -> "기록을 삭제하지 못했어요. 잠시 후 다시 시도해주세요."
                    }
                    state.update { it.copy(detail = it.detail.copy(error = message)) }
                    // 이미 없는 기록이면 목록을 다시 불러와 맞춘다
                    if (error is com.kiri.domain.review.model.ReviewNotFoundException) loadReviews(id)
                }
            state.update { it.copy(detail = it.detail.copy(saving = false)) }
        }
    }

    private fun Throwable.isRetryable() =
        this !is CancelledScheduleException && this !is ScheduleNotFoundException && this !is NoFamilyException

    private fun Throwable.message() = when (this) {
        is CancelledScheduleException -> "취소된 약속이에요."
        is ScheduleNotFoundException -> "찾을 수 없는 약속이에요."
        is NoFamilyException -> "가족 공간이 없어요."
        else -> "약속 정보를 불러오지 못했어요."
    }

    companion object {
        const val SCHEDULE_ID_ARG = "scheduleId"
        private const val EDITING_REVIEW_ID = "editingReviewId"
        private const val EDITING_PHOTO_URLS = "editingPhotoUrls"
        private const val EDITING_PHOTO_KEYS = "editingPhotoKeys"
        // "2026-11-17" 형식
        const val DATE_ARG = "date"
    }
}

// 일정 상세의 미션명. 오지 않으면 "11월 17일 약속" 처럼 날짜로 만든다. 일정 상세·수정·응답·취소 화면의 본문 제목에 쓴다
internal fun ScheduleDetail.displayTitle() = missionTitle ?: "${date.monthValue}월 ${date.dayOfMonth}일 약속"

// 날짜·시간·참여자·상태·미션명은 일정에서, 장소·걷기량·활동시간·필수 활동(단계)·준비물은 미션에서 가져온다.
// 미션을 불러오지 못하면(행사 취소·숨김으로 404 등) 일정의 미션명을 쓰고, 그것도 없으면 날짜로 만든다. 나머지는 비워 둔다
internal fun ScheduleDetail.toMissionDetail(scheduleId: Long, mission: MissionInfo? = null) = MissionDetail(
    id = scheduleId.toString(),
    title = mission?.title ?: displayTitle(),
    familyName = "",
    date = date,
    startTime = time?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
    place = mission?.place?.name,
    expectedSteps = null,
    durationMinutes = mission?.durationMinMinutes,
    participants = participants.map { DetailParticipant(it.nickname, it.accepted, it.characterImageUrl) },
    completed = status == ScheduleDisplayStatus.COMPLETED,
    activities = mission?.steps.orEmpty().map { it.toSuggestion() },
    essentials = emptyList(),
    nearby = emptyList(),
    walkingLevel = mission?.walkingLevel?.label,
    durationMaxMinutes = mission?.durationMaxMinutes,
)

private fun MissionGuide.toSuggestion() = DetailSuggestion(title, description.orEmpty())

// 캘린더와 같이 당일(TODAY)은 확정으로 보여준다. 당일 문구는 홈 화면에서 쓴다
internal fun ScheduleDisplayStatus?.badgeLabel() = when (this) {
    ScheduleDisplayStatus.SCHEDULED -> "약속 예정"
    ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.TODAY -> "약속 확정"
    ScheduleDisplayStatus.COMPLETED -> "약속 완료"
    null -> "약속"
}

// 약속 예정·완료는 회색 배지, 확정·당일은 초록 배지로 보여준다
internal fun ScheduleDisplayStatus?.usesNeutralBadge() =
    this == ScheduleDisplayStatus.SCHEDULED || this == ScheduleDisplayStatus.COMPLETED

// 리뷰 작성자의 가족 역할을 기록 카드에 쓰는 말로 바꾼다. 모르면 빈 문자열이다
internal val FamilyRole?.label: String
    get() = when (this) {
        FamilyRole.MOTHER -> "엄마"
        FamilyRole.FATHER -> "아빠"
        FamilyRole.DAUGHTER -> "딸"
        FamilyRole.SON -> "아들"
        null -> ""
    }

// 리뷰를 쓰지 않는 테스트·미리보기에서 쓴다. 리뷰가 없는 것으로 본다
private object NoReviews : ScheduleReviewRepository {
    override suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> = Result.success(emptyList())
}

// 컨디션 커스텀 조회로 받은 미션 제목·걷기량·활동시간을 상세에 덮어쓴다
internal fun MissionDetail.withConditionCustom(custom: ConditionCustom) = copy(
    title = custom.missionTitle.ifBlank { title },
    walkingLevel = custom.walkingLevel?.label ?: walkingLevel,
    durationMinutes = custom.durationMinMinutes ?: durationMinutes,
    durationMaxMinutes = custom.durationMaxMinutes ?: durationMaxMinutes,
    conditions = custom.conditions,
)

// 컨디션 커스텀을 쓰지 않는 테스트·미리보기에서 쓴다. 정보를 불러오지 못한 것으로 본다
private object NoConditions : ScheduleConditionRepository {
    override suspend fun getConditionCustom(scheduleId: Long): Result<ConditionCustom> =
        Result.failure(UnsupportedOperationException())

    override suspend fun applyConditions(scheduleId: Long, conditions: Set<com.kiri.domain.mission.model.Condition>): Result<Unit> =
        Result.failure(UnsupportedOperationException())
}

// 근처 놀거리를 쓰지 않는 테스트·미리보기에서 쓴다. 추천할 장소가 없는 것으로 본다
private object NoNearby : ScheduleNearbyRepository {
    override suspend fun getNearby(scheduleId: Long): Result<List<NearbyPlace>> = Result.success(emptyList())
}
