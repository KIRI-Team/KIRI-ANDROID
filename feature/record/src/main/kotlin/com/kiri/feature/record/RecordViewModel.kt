package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.domain.repository.BucketItemRepository
import com.kiri.domain.repository.UpcomingScheduleRepository
import com.kiri.domain.model.schedule.UpcomingSchedules
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.model.bucket.BucketItemNotFoundException
import com.kiri.domain.model.bucket.CompletedBucketItemException
import com.kiri.domain.model.bucket.NotBucketItemOwnerException
import com.kiri.domain.model.bucket.ScheduledBucketItemException
import kotlinx.coroutines.Job
import com.kiri.domain.record.*
import com.kiri.domain.time.kiriToday
import com.kiri.feature.record.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class RecordViewModel
@Inject
constructor(
    private val savedState: SavedStateHandle,
    private val repository: RecordRepository,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val calendarSchedules: CalendarScheduleRepository = NoCalendarSchedules,
    private val bucketItems: BucketItemRepository = NoBucketItems,
    private val upcoming: UpcomingScheduleRepository = NoUpcomingSchedules,
) : ViewModel() {
    private val today
        get() = clock.kiriToday()

    private val initial = RecordUiState(today = today, loading = true)
    private val _uiState =
        MutableStateFlow(
            initial.copy(
                tab =
                    savedState.get<String>("recordTab")?.let { value ->
                        RecordTab.entries.firstOrNull { it.name == value }
                    } ?: RecordTab.CALENDAR,
                month =
                    savedState.get<String>("recordMonth")?.let(YearMonth::parse) ?: initial.month,
                selectedDate = savedState.get<String>("recordDate")?.let(LocalDate::parse) ?: today,
                selectedSaverId = savedState.get<String>("recordSaver")?.takeIf { it.toLongOrNull() != null },
            )
        )
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _effect = Channel<RecordUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var refreshing = false

    // 기기에 저장된 활동으로 만든 날짜별 표시. 서버 일정 표시와 합쳐 calendarMarks 가 된다
    private var localMarks: Map<LocalDate, RecordCalendarMark> = emptyMap()

    // 서버에서 받은 달별 가족 일정. 달을 오가도 다시 받지 않는다 (다시 시도하면 새로 받는다)
    private val schedulesByMonth = mutableMapOf<YearMonth, List<CalendarSchedule>>()
    private val loadingMonths = mutableSetOf<YearMonth>()

    // 버킷리스트 다음 페이지를 부를 때 넣는 값과, 지금 불러오는 요청
    private var bucketCursor: Long? = null
    private var bucketJob: Job? = null

    // 지금 불러오는 다가오는 일정 요청. 새로 고치면 이전 요청은 버린다
    private var upcomingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.records.collect { records ->
                localMarks = localCalendarMarks(records)
                update { state ->
                    val entries = records.entries
                    state.copy(
                        entries = entries,
                        currentUserId = records.currentUserId,
                        savers = records.savers.map { RecordSaver(it.key, it.value) },
                        buckets =
                            entries
                                .filter { it.kind == CalendarEntryKind.BUCKET }
                                .map {
                                    RecordBucketItem(
                                        it.id,
                                        it.title,
                                        it.saverId.orEmpty(),
                                        it.completed,
                                    )
                                },
                        schedules =
                            entries
                                .filter {
                                    it.kind == CalendarEntryKind.SCHEDULE &&
                                        (it.status == com.kiri.domain.home.ScheduleStatus.CONFIRMED)
                                }
                                .map {
                                    RecordSchedule(
                                        it.id,
                                        it.title,
                                        it.date,
                                        RecordScheduleStatus.CONFIRMED,
                                    )
                                },
                        questions = records.questions,
                        calendarPhotos =
                            entries
                                .filter { it.kind == CalendarEntryKind.MEMORY }
                                .sortedWith(compareBy({ it.startTime }, { it.id }))
                                .mapNotNull { entry ->
                                    records.photos[entry.id]
                                        ?.find { it.id == entry.representativePhotoId }
                                        ?.let { entry.date to it }
                                }
                                .groupBy({ it.first }, { it.second })
                                .mapValues { it.value.first() },
                    ).withServerSchedules()
                }
            }
        }
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        if (refreshing) return@launch
        refreshing = true
        _uiState.update {
            val now = clock.kiriToday()
            val followToday = it.selectedDate == it.today && it.month == YearMonth.from(it.today)
            it.copy(
                loading = true,
                error = null,
                today = now,
                selectedDate = if (followToday) now else it.selectedDate,
                month = if (followToday) YearMonth.from(now) else it.month,
            ).withServerSchedules()
        }
        loadMonth(uiState.value.month, force = true)
        loadUpcoming()
        loadBuckets()
        try {
            repository.refresh()
            _uiState.update { it.copy(loading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update { it.copy(loading = false, error = "기록을 불러오지 못했어요.") }
        } finally {
            refreshing = false
        }
    }

    fun onEvent(event: RecordUiEvent) {
        when (event) {
            is RecordUiEvent.TabSelected -> update { it.copy(tab = event.tab) }
            RecordUiEvent.PreviousMonth -> changeMonth(-1)
            RecordUiEvent.NextMonth -> changeMonth(1)
            is RecordUiEvent.DateSelected ->
                if (YearMonth.from(event.date) == uiState.value.month) {
                    update { it.copy(selectedDate = event.date) }
                    openSelectedDay()
                }
            is RecordUiEvent.SaverSelected -> {
                val state = uiState.value
                val known = event.saverId == null || state.bucketSavers.any { it.memberId.toString() == event.saverId }
                if (known && event.saverId != state.selectedSaverId) {
                    update { it.copy(selectedSaverId = event.saverId) }
                    loadBuckets()
                }
            }
            is RecordUiEvent.ScheduleClicked ->
                if (uiState.value.schedules.any { it.id == event.id })
                    _effect.trySend(RecordUiEffect.OpenSchedule(event.id))
            is RecordUiEvent.BucketClicked ->
                if (uiState.value.bucketItems.any { it.id.toString() == event.id })
                    _effect.trySend(RecordUiEffect.OpenBucket(event.id))
            RecordUiEvent.BucketListEndReached -> loadBuckets(more = true)
            RecordUiEvent.BucketListRetryClicked -> loadBuckets(more = uiState.value.bucketItems.isNotEmpty())
            is RecordUiEvent.BucketDeleteConfirmed -> deleteBucket(event.id)
            is RecordUiEvent.CalendarEntryClicked ->
                if (uiState.value.entries.any { it.id == event.id })
                    _effect.trySend(RecordUiEffect.OpenEntry(event.id))
            RecordUiEvent.RetryClicked -> refresh()
            is RecordUiEvent.ServerScheduleClicked ->
                uiState.value.upcomingSchedules
                    .firstOrNull { it.id == event.scheduleId }
                    ?.let { _effect.trySend(RecordUiEffect.OpenServerSchedule(it.id, it.date)) }
        }
    }

    // 가족 응답을 기다리는 일정 요청은 "약속 예정"으로 보여준다
    private val CalendarEntry.pending
        get() = status == ScheduleStatus.PENDING_RESPONSE

    private fun changeMonth(offset: Long) {
        val month = runCatching { uiState.value.month.plusMonths(offset) }.getOrNull() ?: return
        update {
            it.copy(
                month = month,
                selectedDate = month.atDay(minOf(it.selectedDate.dayOfMonth, month.lengthOfMonth())),
            ).withServerSchedules()
        }
        loadMonth(month)
    }

    // 그 달의 서버 일정을 불러온다. 이미 받았거나 받는 중이면 다시 요청하지 않는다
    // 응답이 오기 전에 다른 달로 넘어가도 받은 일정은 그 달 몫으로 기억하고, 보고 있는 달일 때만 화면에 반영한다
    private fun loadMonth(month: YearMonth, force: Boolean = false) {
        if (month in loadingMonths || (!force && month in schedulesByMonth)) return
        loadingMonths += month
        viewModelScope.launch {
            calendarSchedules.getMonthlySchedules(month)
                .onSuccess { schedules ->
                    schedulesByMonth[month] = schedules
                    _uiState.update { it.withServerSchedules() }
                }
                .onFailure {
                    _uiState.update { state ->
                        if (state.month == month) state.copy(error = "일정을 불러오지 못했어요.") else state
                    }
                }
            loadingMonths -= month
        }
    }

    // 고른 날의 활동(기기에 저장된 활동과 열 수 있는 서버 일정) 중 가장 이른 것 하나의 상세 화면으로 간다
    // 날짜 아래에 그날 활동 목록을 따로 보여주지 않으므로, 여러 개가 겹치면 시간이 가장 이른 것을 연다
    // 시간을 모르는 활동은 뒤로 미루고, 시간이 같으면 기기에 저장된 활동을 먼저 연다. 취소된 일정만 있으면 이동하지 않는다
    private fun openSelectedDay() {
        val state = uiState.value
        val candidates: List<Pair<java.time.LocalTime?, RecordUiEffect>> =
            state.selectedEntries.map { it.startTime to RecordUiEffect.OpenEntry(it.id) } +
                state.selectedSchedules
                    .filter { it.canOpen }
                    .map { it.time to RecordUiEffect.OpenServerSchedule(it.id, it.date) }
        candidates.minByOrNull { it.first ?: java.time.LocalTime.MAX }?.let { _effect.trySend(it.second) }
    }

    // 다가오는 일정 카드(응답 필요 1건, 다가오는 1건)를 불러온다. 실패하면 이전 카드를 지우고 실패 문구를 보여준다
    private fun loadUpcoming() {
        upcomingJob?.cancel()
        upcomingJob = viewModelScope.launch {
            upcoming.getUpcomingSchedules()
                .onSuccess { result ->
                    _uiState.update { it.copy(upcomingSchedules = result.cards, upcomingFailed = false) }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { it.copy(upcomingSchedules = emptyList(), upcomingFailed = true) }
                }
        }
    }

    private fun RecordUiState.withServerSchedules(): RecordUiState {
        val schedules = schedulesByMonth[month].orEmpty()
        return copy(
            monthSchedules = schedules,
            calendarMarks = mergeCalendarMarks(localMarks, schedules.toCalendarMarks()),
        )
    }

    // 사진 기록 > 약속 > 버킷 순으로 하나만 고르고, 활동이 없는 날은 가족 모두 답한 질문을 보여준다
    private fun localCalendarMarks(records: RecordCollection): Map<LocalDate, RecordCalendarMark> {
        val entries = records.entries
        val activityMarks =
            entries.groupBy { it.date }.mapValues { (_, values) ->
                val first =
                    values.minWith(
                        compareBy<CalendarEntry> {
                                when (it.kind) {
                                    CalendarEntryKind.MEMORY -> 0
                                    CalendarEntryKind.SCHEDULE -> 1
                                    CalendarEntryKind.BUCKET -> 2
                                }
                            }
                            .thenBy { it.startTime }
                            .thenBy { it.id }
                    )
                when (first.kind) {
                    CalendarEntryKind.SCHEDULE ->
                        if (values.any { it.kind == CalendarEntryKind.SCHEDULE && it.status == ScheduleStatus.CONFIRMED })
                            RecordCalendarMark.CONFIRMED
                        else RecordCalendarMark.PLANNED
                    CalendarEntryKind.BUCKET -> RecordCalendarMark.BUCKET
                    CalendarEntryKind.MEMORY -> RecordCalendarMark.PHOTO_FIRST
                }
            }
        val questionMarks =
            records.questions
                .filterValues { it.everyoneAnswered }
                .keys
                .filter { date -> entries.none { it.date == date } }
                .associateWith { RecordCalendarMark.QUESTION }
        return activityMarks + questionMarks
    }

    // 가족 버킷리스트를 불러온다. more 가 false 면 고른 구성원 기준으로 첫 페이지부터 다시 받고(진행 중인 요청은 버린다),
    // true 면 다음 페이지를 이어 붙인다. 이미 받는 중이거나 마지막 페이지면 다음 페이지는 요청하지 않는다
    private fun loadBuckets(more: Boolean = false) {
        val state = uiState.value
        if (more && (!state.bucketHasNext || bucketJob?.isActive == true)) return
        bucketJob?.cancel()
        val memberId = state.selectedSaverId?.toLongOrNull()
        val cursor = if (more) bucketCursor else null
        _uiState.update {
            if (more) it.copy(bucketLoadingMore = true, bucketError = null)
            else it.copy(bucketLoading = true, bucketLoadingMore = false, bucketError = null)
        }
        bucketJob = viewModelScope.launch {
            bucketItems.getBucketItems(memberId, cursor, BUCKET_PAGE_SIZE)
                .onSuccess { page ->
                    bucketCursor = page.nextCursor
                    _uiState.update {
                        it.copy(
                            bucketSavers = page.savers,
                            bucketItems = if (more) (it.bucketItems + page.items).distinctBy { item -> item.id } else page.items,
                            bucketHasNext = page.hasNext && page.nextCursor != null,
                            bucketLoading = false,
                            bucketLoadingMore = false,
                        )
                    }
                    // 고른 구성원이 가족에서 빠졌으면(탈퇴 등) 전체로 돌린다
                    val selected = uiState.value.selectedSaverId
                    if (selected != null && page.savers.none { it.memberId.toString() == selected }) {
                        update { it.copy(selectedSaverId = null) }
                        loadBuckets()
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(bucketLoading = false, bucketLoadingMore = false, bucketError = "버킷리스트를 불러오지 못했어요.")
                    }
                }
        }
    }

    // 내가 저장한 버킷을 지운다. 성공하면 목록에서 빼고 저장한 사람의 칩 개수를 하나 줄인다 (페이지는 그대로 둔다)
    // 이미 없거나 상태가 바뀐 버킷이라 실패하면 목록이 오래된 것이므로 첫 페이지부터 다시 불러온다
    private fun deleteBucket(id: String) {
        val item = uiState.value.bucketItems.find { it.id.toString() == id } ?: return
        if (!item.deletable || uiState.value.bucketDeletingId != null) return
        _uiState.update { it.copy(bucketDeletingId = item.id) }
        viewModelScope.launch {
            bucketItems.deleteBucketItem(item.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            bucketDeletingId = null,
                            bucketItems = state.bucketItems.filterNot { it.id == item.id },
                            bucketSavers = state.bucketSavers.map {
                                if (it.memberId == item.saverId) it.copy(bucketCount = (it.bucketCount - 1).coerceAtLeast(0)) else it
                            },
                        )
                    }
                    _effect.send(RecordUiEffect.ShowNotice("버킷리스트에서 삭제했어요."))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(bucketDeletingId = null) }
                    _effect.send(RecordUiEffect.ShowNotice(error.bucketDeleteMessage()))
                    if (error is BucketItemNotFoundException || error is ScheduledBucketItemException ||
                        error is CompletedBucketItemException || error is NotBucketItemOwnerException
                    ) loadBuckets()
                }
        }
    }

    private fun Throwable.bucketDeleteMessage() = when (this) {
        is BucketItemNotFoundException -> "이미 삭제됐거나 찾을 수 없는 버킷이에요."
        is NotBucketItemOwnerException -> "내가 저장한 버킷만 삭제할 수 있어요."
        is ScheduledBucketItemException -> "일정이 잡힌 버킷은 일정을 취소한 뒤 삭제할 수 있어요."
        is CompletedBucketItemException -> "완료한 버킷은 삭제할 수 없어요."
        else -> "버킷을 삭제하지 못했어요. 다시 시도해주세요."
    }

    private fun update(transform: (RecordUiState) -> RecordUiState) {
        _uiState.update(transform)
        val state = uiState.value
        savedState["recordTab"] = state.tab.name
        savedState["recordMonth"] = state.month.toString()
        savedState["recordDate"] = state.selectedDate.toString()
        savedState["recordSaver"] = state.selectedSaverId
    }
}

private const val BUCKET_PAGE_SIZE = 20

// 서버 버킷리스트를 쓰지 않는 테스트·미리보기에서 쓴다
private object NoBucketItems : BucketItemRepository {
    override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?): Result<BucketItemPage> =
        Result.success(BucketItemPage.Empty)

    override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> =
        Result.failure(BucketItemNotFoundException())
}

// 다가오는 일정을 쓰지 않는 테스트·미리보기에서 쓴다
private object NoUpcomingSchedules : UpcomingScheduleRepository {
    override suspend fun getUpcomingSchedules(): Result<UpcomingSchedules> = Result.success(UpcomingSchedules())
}

// 서버 일정을 쓰지 않는 테스트·미리보기에서 쓴다. 상세는 없는 일정으로 본다
private object NoCalendarSchedules : CalendarScheduleRepository {
    override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
        Result.success(emptyList())

    override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> =
        Result.failure(ScheduleNotFoundException())

    override suspend fun updateSchedule(scheduleId: Long, update: com.kiri.domain.model.schedule.ScheduleUpdate): Result<Unit> =
        Result.failure(ScheduleNotFoundException())

    override suspend fun createSchedule(bucketItemId: Long, form: com.kiri.domain.model.schedule.ScheduleUpdate): Result<Long> =
        Result.failure(ScheduleNotFoundException())
}
