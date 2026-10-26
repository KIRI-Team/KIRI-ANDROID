package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.record.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.kiri.feature.record.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@HiltViewModel
class RecordViewModel @Inject constructor(private val savedState: SavedStateHandle, private val repository: RecordRepository) : ViewModel() {
    private val today = savedState.get<String>("recordSeedDate")?.let(LocalDate::parse)
        ?: LocalDate.now(ZoneId.of("Asia/Seoul")).also { savedState["recordSeedDate"] = it.toString() }
    private val initial = RecordUiState(today = today, loading = true)
    private val _uiState = MutableStateFlow(initial.copy(
        tab = savedState.get<String>("recordTab")?.let { value -> RecordTab.entries.firstOrNull { it.name == value } } ?: RecordTab.CALENDAR,
        month = savedState.get<String>("recordMonth")?.let(YearMonth::parse) ?: initial.month,
        selectedDate = savedState.get<String>("recordDate")?.let(LocalDate::parse) ?: today,
        selectedSaverId = savedState.get<String>("recordSaver"),
    ))
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _effect = Channel<RecordUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var refreshing = false

    init {
        viewModelScope.launch {
            repository.records.collect { records ->
                update { state ->
                    val entries = records.entries
                    state.copy(
                        entries = entries,
                        currentUserId = records.currentUserId,
                        savers = records.savers.map { RecordSaver(it.key, it.value) },
                        buckets = entries.filter { it.kind == CalendarEntryKind.BUCKET }.map { RecordBucketItem(it.id, it.title, it.saverId.orEmpty(), it.completed) },
                        schedules = entries.filter { it.kind == CalendarEntryKind.SCHEDULE && (it.status == com.kiri.domain.home.ScheduleStatus.CONFIRMED) }.map { RecordSchedule(it.id, it.title, it.date, RecordScheduleStatus.CONFIRMED) },
                        calendarMarks = entries.groupBy { it.date }.mapValues { (_, values) ->
                            when (values.minWith(compareBy<CalendarEntry> { when (it.kind) { CalendarEntryKind.MEMORY -> 0; CalendarEntryKind.SCHEDULE -> 1; CalendarEntryKind.BUCKET -> 2 } }.thenBy { it.startTime }.thenBy { it.id }).kind) {
                                CalendarEntryKind.SCHEDULE -> if (values.any { it.kind == CalendarEntryKind.SCHEDULE && (it.status == com.kiri.domain.home.ScheduleStatus.CONFIRMED) }) RecordCalendarMark.CONFIRMED else RecordCalendarMark.PLANNED
                                CalendarEntryKind.BUCKET -> RecordCalendarMark.BUCKET
                                CalendarEntryKind.MEMORY -> RecordCalendarMark.PHOTO_FIRST
                            }
                        } + records.questions.filterValues { it.everyoneAnswered }.keys.filter { date -> entries.none { it.date == date } }.associateWith { RecordCalendarMark.QUESTION },
                        questions = records.questions,
                        calendarPhotos = entries.filter { it.kind == CalendarEntryKind.MEMORY }.sortedWith(compareBy({ it.startTime }, { it.id })).mapNotNull { entry ->
                            records.photos[entry.id]?.find { it.id == entry.representativePhotoId }?.let { entry.date to it }
                        }.groupBy({ it.first }, { it.second }).mapValues { it.value.first() },
                    )
                }
            }
        }
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        if (refreshing) return@launch
        refreshing = true
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            repository.refresh()
            _uiState.update { it.copy(loading = false) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _uiState.update { it.copy(loading = false, error = "기록을 불러오지 못했어요.") } }
        finally { refreshing = false }
    }

    fun onEvent(event: RecordUiEvent) {
        when (event) {
            is RecordUiEvent.TabSelected -> update { it.copy(tab = event.tab) }
            RecordUiEvent.PreviousMonth -> changeMonth(-1)
            RecordUiEvent.NextMonth -> changeMonth(1)
            is RecordUiEvent.DateSelected -> if (YearMonth.from(event.date) == uiState.value.month) update { it.copy(selectedDate = event.date) }
            is RecordUiEvent.SaverSelected -> if (event.saverId == null || uiState.value.savers.any { it.id == event.saverId }) update { it.copy(selectedSaverId = event.saverId) }
            is RecordUiEvent.ScheduleClicked -> if (uiState.value.schedules.any { it.id == event.id }) _effect.trySend(RecordUiEffect.OpenSchedule(event.id))
            is RecordUiEvent.BucketClicked -> if (uiState.value.buckets.any { it.id == event.id }) _effect.trySend(RecordUiEffect.OpenBucket(event.id))
            is RecordUiEvent.CalendarEntryClicked -> if (uiState.value.entries.any { it.id == event.id }) _effect.trySend(RecordUiEffect.OpenEntry(event.id))
            RecordUiEvent.RetryClicked -> refresh()
        }
    }

    // 가족 응답을 기다리는 일정 요청은 "약속 예정"으로 보여준다
    private val CalendarEntry.pending get() = status == ScheduleStatus.PENDING_RESPONSE

    private fun changeMonth(offset: Long) {
        val month = runCatching { uiState.value.month.plusMonths(offset) }.getOrNull() ?: return
        update { it.copy(month = month, selectedDate = month.atDay(minOf(it.selectedDate.dayOfMonth, month.lengthOfMonth()))) }
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
