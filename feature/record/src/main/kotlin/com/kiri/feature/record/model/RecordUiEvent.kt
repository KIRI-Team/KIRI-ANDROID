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

    // 고른 날의 서버 일정 중 하나를 눌러 상세 화면으로 간다
    data class ServerScheduleClicked(val scheduleId: Long) : RecordUiEvent
}

sealed interface RecordUiEffect {
    data class OpenSchedule(val id: String) : RecordUiEffect
    data class OpenBucket(val id: String) : RecordUiEffect
    data class OpenEntry(val id: String) : RecordUiEffect

    // 서버 일정 상세 화면을 연다
    // 날짜는 상세를 받기 전에도 앱바 제목을 보여주려고 함께 넘긴다
    data class OpenServerSchedule(val scheduleId: Long, val date: java.time.LocalDate) : RecordUiEffect
}
