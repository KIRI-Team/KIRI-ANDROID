package com.kiri.feature.record.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.bucket.BucketItem
import com.kiri.domain.model.bucket.BucketSaver
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.domain.record.CalendarEntry
import com.kiri.domain.record.RecordPhoto

enum class RecordTab(val label: String) { CALENDAR("캘린더"), BUCKET("버킷리스트") }
// CANCELLED 는 취소된 약속만 있는 날로, 흐리게 그린다
enum class RecordCalendarMark { QUESTION, PLANNED, CONFIRMED, CANCELLED, BUCKET, PHOTO_FIRST, PHOTO_SECOND }
enum class RecordScheduleStatus(val label: String) { PLANNED("약속 예정"), CONFIRMED("약속 확정") }
data class RecordSaver(val id: String, val nickname: String)
data class RecordBucketItem(val id: String, val title: String, val saverId: String, val completed: Boolean = false)
data class RecordSchedule(val id: String, val title: String, val date: LocalDate, val status: RecordScheduleStatus)

data class RecordUiState(
    val today: LocalDate,
    val month: YearMonth = YearMonth.from(today),
    val selectedDate: LocalDate = today,
    val tab: RecordTab = RecordTab.CALENDAR,
    // 버킷리스트에서 고른 구성원의 서버 회원 id. null 이면 전체
    val selectedSaverId: String? = null,
    val currentUserId: String = "",
    val savers: List<RecordSaver> = emptyList(),
    val buckets: List<RecordBucketItem> = emptyList(),
    val schedules: List<RecordSchedule> = emptyList(),
    val calendarMarks: Map<LocalDate, RecordCalendarMark> = emptyMap(),
    val entries: List<CalendarEntry> = emptyList(),
    val calendarPhotos: Map<LocalDate, RecordPhoto> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
    val questions: Map<LocalDate, com.kiri.domain.record.RecordedQuestion> = emptyMap(),
    // 서버에서 받은 보고 있는 달의 가족 일정. 상세는 일정 상세 화면에서 id 로 따로 조회한다
    val monthSchedules: List<CalendarSchedule> = emptyList(),
    // 다가오는 일정 조회로 받은 카드. 응답이 필요한 일정이 먼저 온다 (최대 2건)
    val upcomingSchedules: List<UpcomingSchedule> = emptyList(),
    // 다가오는 일정을 불러오지 못했다
    val upcomingFailed: Boolean = false,
    // 서버에서 받은 가족 버킷리스트. 칩에 쓰는 구성원(저장 개수 포함)과 지금까지 불러온 페이지의 항목이다
    // 항목은 고른 구성원으로 이미 걸러져 있다
    val bucketSavers: List<BucketSaver> = emptyList(),
    val bucketItems: List<BucketItem> = emptyList(),
    val bucketHasNext: Boolean = false,
    val bucketLoading: Boolean = false,
    val bucketLoadingMore: Boolean = false,
    val bucketError: String? = null,
    // 삭제 중인 버킷. 그동안 같은 버킷을 다시 지우지 않는다
    val bucketDeletingId: Long? = null,
) {
    // 고른 날의 서버 일정. 시간 순이고 시간을 모르는 일정은 뒤에 둔다. 날짜를 누르면 이 중 하나로 이동한다
    val selectedSchedules
        get() = monthSchedules.filter { it.date == selectedDate }
            .sortedWith(compareBy({ it.time ?: java.time.LocalTime.MAX }, { it.id }))

    val selectedEntries get() = entries.filter { it.date == selectedDate }.sortedWith(compareBy({ it.startTime ?: java.time.LocalTime.MAX }, { it.id }))
    fun saverName(id: String) = savers.firstOrNull { it.id == id }?.nickname ?: "탈퇴한 멤버"
}

// 취소된 일정은 서버가 상세를 돌려주지 않아 상세 화면을 열 수 없다
val CalendarSchedule.canOpen get() = status != CalendarScheduleStatus.CANCELLED

// 서버 일정을 날짜별 표시로 바꾼다. 확정(완료 포함) > 예정 > 취소 순으로 하나만 고른다
internal fun List<CalendarSchedule>.toCalendarMarks(): Map<LocalDate, RecordCalendarMark> =
    groupBy { it.date }.mapValues { (_, schedules) ->
        val statuses = schedules.map { it.status }
        when {
            statuses.any { it == CalendarScheduleStatus.CONFIRMED || it == CalendarScheduleStatus.COMPLETED } -> RecordCalendarMark.CONFIRMED
            statuses.any { it != CalendarScheduleStatus.CANCELLED } -> RecordCalendarMark.PLANNED
            else -> RecordCalendarMark.CANCELLED
        }
    }

// 기기에 저장된 활동 표시와 서버 일정 표시를 합친다. 사진 기록이 있는 날은 사진을 그대로 보여주고,
// 나머지는 확정 > 예정 > 버킷 > 가족 질문 > 취소 순으로 하나만 고른다
internal fun mergeCalendarMarks(
    local: Map<LocalDate, RecordCalendarMark>,
    server: Map<LocalDate, RecordCalendarMark>,
): Map<LocalDate, RecordCalendarMark> = (local.keys + server.keys).associateWith { date ->
    listOfNotNull(local[date], server[date]).maxBy { it.mergeRank }
}

private val RecordCalendarMark.mergeRank
    get() = when (this) {
        RecordCalendarMark.CANCELLED -> 0
        RecordCalendarMark.QUESTION -> 1
        RecordCalendarMark.BUCKET -> 2
        RecordCalendarMark.PLANNED -> 3
        RecordCalendarMark.CONFIRMED -> 4
        RecordCalendarMark.PHOTO_FIRST, RecordCalendarMark.PHOTO_SECOND -> 5
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
            upcomingSchedules = listOf(
                UpcomingSchedule(1, today.plusDays(3), java.time.LocalTime.of(18, 0), UpcomingScheduleType.RESPONSE_REQUIRED, 3, "한강 뚜벅뚜벅 축제 가보기", null),
                UpcomingSchedule(2, today.plusDays(1), java.time.LocalTime.of(14, 50), UpcomingScheduleType.UPCOMING, 1, "광장시장에서 시장음식 먹어보기", null),
            ),
            calendarPhotos = mapOf(
                month.atDay(11) to RecordPhoto("preview-group", "demo:group"),
                month.atDay(20) to RecordPhoto("preview-pixel", "demo:pixel"),
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
