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

    // 버킷리스트 끝에 가까워지면 다음 페이지를 불러온다
    data object BucketListEndReached : RecordUiEvent

    // 불러오지 못한 버킷리스트(첫 페이지 또는 다음 페이지)를 다시 불러온다
    data object BucketListRetryClicked : RecordUiEvent

    // 삭제 확인 창에서 삭제하기를 눌렀다
    data class BucketDeleteConfirmed(val id: String) : RecordUiEvent

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

    // 화면 위에 잠깐 띄우는 안내 (버킷 삭제 결과 등)
    data class ShowNotice(val message: String) : RecordUiEffect
}
