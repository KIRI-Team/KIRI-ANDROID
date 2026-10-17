package com.kiri.feature.record.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import com.kiri.domain.record.CalendarEntry
import com.kiri.domain.record.RecordPhoto

enum class RecordTab(val label: String) { CALENDAR("캘린더"), BUCKET("버킷리스트") }
enum class RecordCalendarMark { QUESTION, PLANNED, CONFIRMED, BUCKET, PHOTO_FIRST, PHOTO_SECOND }
enum class RecordScheduleStatus(val label: String) { PLANNED("약속 예정"), CONFIRMED("약속 확정") }
data class RecordSaver(val id: String, val nickname: String)
data class RecordBucketItem(val id: String, val title: String, val saverId: String, val completed: Boolean = false)
data class RecordSchedule(val id: String, val title: String, val date: LocalDate, val status: RecordScheduleStatus)

data class RecordUiState(
    val today: LocalDate,
    val month: YearMonth = YearMonth.from(today),
    val selectedDate: LocalDate = today,
    val tab: RecordTab = RecordTab.CALENDAR,
    val selectedSaverId: String? = null,
    val savers: List<RecordSaver> = emptyList(),
    val buckets: List<RecordBucketItem> = emptyList(),
    val schedules: List<RecordSchedule> = emptyList(),
    val calendarMarks: Map<LocalDate, RecordCalendarMark> = emptyMap(),
    val entries: List<CalendarEntry> = emptyList(),
    val calendarPhotos: Map<LocalDate, RecordPhoto> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val visibleBuckets get() = buckets.filter { selectedSaverId == null || it.saverId == selectedSaverId }
    val upcomingSchedules get() = schedules.filter { !it.date.isBefore(today) }.sortedBy { it.date }
    fun saverName(id: String) = savers.firstOrNull { it.id == id }?.nickname ?: "탈퇴한 멤버"
}

/** Monday-first, padded complete weeks. Includes every day, including leap day. */
fun calendarWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    return (days + List<LocalDate?>((7 - days.size % 7) % 7) { null }).chunked(7)
}

/** UI-only fixtures until the record API is connected; never represent actual family records. */
object RecordPreviewData {
    fun state(today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))): RecordUiState {
        val month = YearMonth.from(today)
        return RecordUiState(
            today = today,
            savers = listOf(RecordSaver("mother", "엄마"), RecordSaver("father", "아빠"), RecordSaver("daughter", "딸")),
            buckets = listOf(
                RecordBucketItem("bucket-1", "광장시장에서 시장음식 먹어보기", "father", completed = true),
                RecordBucketItem("bucket-2", "광장시장에서 시장음식 먹어보기", "father"),
                RecordBucketItem("bucket-3", "광장시장에서 시장음식 먹어보기", "mother"),
                RecordBucketItem("bucket-4", "광장시장에서 시장음식 먹어보기", "daughter"),
            ),
            schedules = listOf(
                RecordSchedule("schedule-1", "한강 뚜벅뚜벅 축제 가보기", today.plusDays(3), RecordScheduleStatus.PLANNED),
                RecordSchedule("schedule-2", "한강 뚜벅뚜벅 축제 가보기", today.plusDays(6), RecordScheduleStatus.CONFIRMED),
                RecordSchedule("schedule-3", "한강 뚜벅뚜벅 축제 가보기", today.plusDays(10), RecordScheduleStatus.CONFIRMED),
            ),
            calendarMarks = mapOf(
                month.atDay(3) to RecordCalendarMark.QUESTION,
                month.atDay(9) to RecordCalendarMark.PLANNED,
                month.atDay(11) to RecordCalendarMark.PHOTO_FIRST,
                month.atDay(12) to RecordCalendarMark.BUCKET,
                month.atDay(20) to RecordCalendarMark.PHOTO_SECOND,
                month.atDay(24) to RecordCalendarMark.CONFIRMED,
            ),
        )
    }
}
