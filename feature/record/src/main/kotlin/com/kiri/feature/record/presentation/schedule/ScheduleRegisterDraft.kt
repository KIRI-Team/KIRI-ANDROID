package com.kiri.feature.record.presentation.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 일정 등록하기 화면에서 사용자가 채우는 값. 날짜·시간·구성원 한 명 이상을 골라야 초대장을 만들 수 있다
data class ScheduleRegisterDraft(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val memberIds: Set<String> = emptySet(),
    val message: String = "",
) {
    val canSubmit: Boolean get() = date != null && time != null && memberIds.isNotEmpty()

    fun withMessage(text: String): ScheduleRegisterDraft = copy(message = text.take(MAX_MESSAGE_LENGTH))

    companion object {
        const val MAX_MESSAGE_LENGTH = 100
    }
}

// Figma 8441:7579 표기 그대로: 26.10.07(수) · 16:30분
internal fun scheduleLabel(date: LocalDate, time: LocalTime): String =
    "${date.format(DateFormat)}(${WeekdayNames.getValue(date.dayOfWeek)}) · ${time.format(TimeFormat)}분"

internal fun memberCountLabel(count: Int): String = "인원 ${count}명"

// 요청알림 카드의 날짜: 26.06.23
internal fun requestDateLabel(date: LocalDate): String = date.format(DateFormat)

// 아빠가 저장했어요! / 딸이 저장했어요!
internal fun savedByLabel(name: String): String = "${name}${subjectParticle(name)} 저장했어요!"

// 아빠가 저장한 미션이에요! / 딸이 저장한 미션이에요!
internal fun savedMissionLabel(name: String): String = "${name}${subjectParticle(name)} 저장한 미션이에요!"

// 카카오톡 등 공유 시트로 보낼 요청 문구. 초대 링크가 생기기 전까지는 요청 내용만 글로 보낸다
internal fun scheduleRequestShareText(
    title: String,
    date: LocalDate,
    time: LocalTime?,
    memberNames: List<String>,
    message: String,
): String = buildString {
    appendLine("[끼리] 일정 요청이 왔어요!")
    appendLine("활동: $title")
    appendLine("일정: ${time?.let { scheduleLabel(date, it) } ?: requestDateLabel(date)}")
    append("함께: ${memberNames.joinToString(", ")}")
    if (message.isNotBlank()) append("\n\n$message")
}

// 마지막 글자에 받침이 있으면 '이', 없거나 한글이 아니면 '가'
internal fun subjectParticle(name: String): String {
    val last = name.lastOrNull() ?: return "가"
    if (last !in '가'..'힣') return "가"
    return if ((last - '가') % FINAL_CONSONANT_COUNT != 0) "이" else "가"
}

private const val FINAL_CONSONANT_COUNT = 28
private val DateFormat = DateTimeFormatter.ofPattern("yy.MM.dd")
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

// 기기 언어와 상관없이 한글 요일을 쓴다
private val WeekdayNames = mapOf(
    DayOfWeek.MONDAY to "월",
    DayOfWeek.TUESDAY to "화",
    DayOfWeek.WEDNESDAY to "수",
    DayOfWeek.THURSDAY to "목",
    DayOfWeek.FRIDAY to "금",
    DayOfWeek.SATURDAY to "토",
    DayOfWeek.SUNDAY to "일",
)
