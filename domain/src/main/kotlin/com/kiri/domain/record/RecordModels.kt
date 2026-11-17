package com.kiri.domain.record

import java.time.LocalDate

// 일정 초대장을 보내는 방법. 앱 알림으로 보내거나, 카카오톡 등 공유 시트로 요청 문구를 함께 보낸다
enum class ScheduleRequestChannel { APP, KAKAO }

data class RecordPhoto(val id: String, val source: String)

data class RecordedQuestion(
    val date: LocalDate,
    val text: String,
    val answers: Map<String, String>,
    val memberNames: Map<String, String>,
    val everyoneAnswered: Boolean,
    val questionId: String? = null,
)
