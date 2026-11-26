package com.kiri.domain.question.model

import java.time.LocalDate

data class RecordedQuestion(
    val date: LocalDate,
    val text: String,
    val answers: Map<String, String>,
    val memberNames: Map<String, String>,
    val everyoneAnswered: Boolean,
    val questionId: String? = null,
)
