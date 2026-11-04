package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.CalendarScheduleRepository
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
                selectedSaverId = savedState.get<String>("recordSaver"),
            )
        )
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _effect = Channel<RecordUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var refreshing = false

    // 기기에 저장된 활동으로 만든 날짜별 표시. 서버 일정 표시와 합쳐 calendarMarks 가 된다
    private var localMarks: Map<LocalDate, RecordCalendarMark> = emptyMap()

    // 서버에서 받은 달별 가족 일정. 달을 오가도 다시 받지 않는다 (다시 시도하면 새로 받는다)
    private val monthSchedules = mutableMapOf<YearMonth, List<CalendarSchedule>>()
    private val loadingMonths = mutableSetOf<YearMonth>()

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
                    openOnlyServerSchedule()
                }
            is RecordUiEvent.SaverSelected ->
                if (event.saverId == null || uiState.value.savers.any { it.id == event.saverId })
                    update { it.copy(selectedSaverId = event.saverId) }
            is RecordUiEvent.ScheduleClicked ->
                if (uiState.value.schedules.any { it.id == event.id })
                    _effect.trySend(RecordUiEffect.OpenSchedule(event.id))
            is RecordUiEvent.BucketClicked ->
                if (uiState.value.buckets.any { it.id == event.id })
                    _effect.trySend(RecordUiEffect.OpenBucket(event.id))
            is RecordUiEvent.CalendarEntryClicked ->
                if (uiState.value.entries.any { it.id == event.id })
                    _effect.trySend(RecordUiEffect.OpenEntry(event.id))
            RecordUiEvent.RetryClicked -> refresh()
            is RecordUiEvent.ServerScheduleClicked ->
                if (uiState.value.selectedSchedules.any { it.id == event.scheduleId && it.canOpen })
                    _effect.trySend(RecordUiEffect.OpenServerSchedule(event.scheduleId))
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
        if (month in loadingMonths || (!force && month in monthSchedules)) return
        loadingMonths += month
        viewModelScope.launch {
            calendarSchedules.getMonthlySchedules(month)
                .onSuccess { schedules ->
                    monthSchedules[month] = schedules
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

    // 고른 날에 열 수 있는 서버 일정이 하나뿐이고 기기에 저장된 활동도 없으면 바로 상세 화면으로 간다
    // 여러 활동이 겹치면 아래 목록에서 직접 고르게 한다
    private fun openOnlyServerSchedule() {
        val state = uiState.value
        val openable = state.selectedSchedules.filter { it.canOpen }
        if (openable.size == 1 && state.selectedEntries.isEmpty())
            _effect.trySend(RecordUiEffect.OpenServerSchedule(openable.single().id))
    }

    private fun RecordUiState.withServerSchedules(): RecordUiState {
        val schedules = monthSchedules[month].orEmpty()
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

    private fun update(transform: (RecordUiState) -> RecordUiState) {
        _uiState.update(transform)
        val state = uiState.value
        savedState["recordTab"] = state.tab.name
        savedState["recordMonth"] = state.month.toString()
        savedState["recordDate"] = state.selectedDate.toString()
        savedState["recordSaver"] = state.selectedSaverId
    }
}

// 서버 일정을 쓰지 않는 테스트·미리보기에서 쓴다. 상세는 없는 일정으로 본다
private object NoCalendarSchedules : CalendarScheduleRepository {
    override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
        Result.success(emptyList())

    override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> =
        Result.failure(ScheduleNotFoundException())
}
