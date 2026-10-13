package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kiri.feature.record.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@HiltViewModel
class RecordViewModel @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val today = savedState.get<String>("recordSeedDate")?.let(LocalDate::parse)
        ?: LocalDate.now(ZoneId.of("Asia/Seoul")).also { savedState["recordSeedDate"] = it.toString() }
    // TODO: Replace UI fixtures with the calendar/bucket repository when its API is available.
    private val initial = RecordPreviewData.state(today)
    private val _uiState = MutableStateFlow(initial.copy(
        tab = savedState.get<String>("recordTab")?.let { value -> RecordTab.entries.firstOrNull { it.name == value } } ?: RecordTab.CALENDAR,
        month = savedState.get<String>("recordMonth")?.let(YearMonth::parse) ?: initial.month,
        selectedDate = savedState.get<String>("recordDate")?.let(LocalDate::parse) ?: today,
        selectedSaverId = savedState.get<String>("recordSaver")?.takeIf { id -> initial.savers.any { it.id == id } },
    ))
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _effect = Channel<RecordUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: RecordUiEvent) {
        when (event) {
            is RecordUiEvent.TabSelected -> update { it.copy(tab = event.tab) }
            RecordUiEvent.PreviousMonth -> changeMonth(-1)
            RecordUiEvent.NextMonth -> changeMonth(1)
            is RecordUiEvent.DateSelected -> if (YearMonth.from(event.date) == uiState.value.month) update { it.copy(selectedDate = event.date) }
            is RecordUiEvent.SaverSelected -> if (event.saverId == null || uiState.value.savers.any { it.id == event.saverId }) update { it.copy(selectedSaverId = event.saverId) }
            is RecordUiEvent.ScheduleClicked -> if (uiState.value.schedules.any { it.id == event.id }) _effect.trySend(RecordUiEffect.OpenSchedule(event.id))
            is RecordUiEvent.BucketClicked -> if (uiState.value.buckets.any { it.id == event.id }) _effect.trySend(RecordUiEffect.OpenBucket(event.id))
        }
    }

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
