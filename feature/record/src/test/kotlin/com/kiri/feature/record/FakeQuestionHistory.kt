package com.kiri.feature.record

import com.kiri.domain.question.repository.QuestionHistoryRepository
import com.kiri.domain.question.model.RecordedQuestion
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

// 기기에 모아 둔 지난 질문 대신 쓰는 테스트용 기록. refreshQuestion 으로 다시 받은 날을 남긴다
internal class FakeQuestionHistory(
    initial: Map<LocalDate, RecordedQuestion> = emptyMap(),
) : QuestionHistoryRepository {
    override val questions = MutableStateFlow(initial)
    val refreshedDates = mutableListOf<LocalDate>()
    var refreshCount = 0

    override suspend fun refresh() {
        refreshCount++
    }

    override suspend fun refreshQuestion(date: LocalDate) {
        refreshedDates += date
    }
}
