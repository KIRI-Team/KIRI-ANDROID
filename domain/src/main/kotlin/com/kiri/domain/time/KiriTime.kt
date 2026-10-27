package com.kiri.domain.time

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

val KiriZone: ZoneId = ZoneId.of("Asia/Seoul")

fun Clock.kiriToday(): LocalDate = LocalDate.now(withZone(KiriZone))

fun Clock.kiriNow(): LocalDateTime = LocalDateTime.now(withZone(KiriZone))
