package com.kiri.feature.mypage.model.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LAST_UPDATED_FORMAT = DateTimeFormatter.ofPattern("yy. MM. dd")

// 약관·라이선스 목록 아래 안내. 예: 최종 업데이트 | 26. 06. 26
fun LocalDate.toLastUpdatedText(): String = "최종 업데이트 | ${format(LAST_UPDATED_FORMAT)}"
