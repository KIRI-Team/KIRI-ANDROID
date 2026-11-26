package com.kiri.domain.question.repository

import com.kiri.domain.question.model.RecordedQuestion
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

// 기록 캘린더에 보여줄 지난 가족 질문. 서버에 날짜별 질문 목록 API 가 없어서, 앱이 받은 오늘의 질문을 날짜별로 기기에 모아 둔다.
// 이미 모아 둔 날의 답변은 질문 id 로 서버에서 다시 받아 최신으로 맞춘다
interface QuestionHistoryRepository {
    val questions: StateFlow<Map<LocalDate, RecordedQuestion>>

    // 오늘의 질문을 받아 모아 둔 기록에 더한다. 둘러보기 중이면 비운다
    suspend fun refresh()

    // 그날 질문의 답변을 서버에서 다시 받는다. 오늘이거나 이미 모아 둔 날만 받는다
    suspend fun refreshQuestion(date: LocalDate) {}
}
