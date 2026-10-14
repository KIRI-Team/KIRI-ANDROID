package com.kiri.feature.record.model

import java.time.LocalDate

sealed interface RecordUiEvent {
    data class TabSelected(val tab: RecordTab) : RecordUiEvent
    data object PreviousMonth : RecordUiEvent
    data object NextMonth : RecordUiEvent
    data class DateSelected(val date: LocalDate) : RecordUiEvent
    data class SaverSelected(val saverId: String?) : RecordUiEvent
    data class ScheduleClicked(val id: String) : RecordUiEvent
    data class BucketClicked(val id: String) : RecordUiEvent
    data class CalendarEntryClicked(val id: String) : RecordUiEvent
    data object RetryClicked : RecordUiEvent
}

sealed interface RecordUiEffect {
    data class OpenSchedule(val id: String) : RecordUiEffect
    data class OpenBucket(val id: String) : RecordUiEffect
    data class OpenEntry(val id: String) : RecordUiEffect
}
