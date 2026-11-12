package com.kiri.domain.repository

// 서비스에 대한 건의를 보낸다
interface SuggestionRepository {
    // 내용(최대 500자)과 첨부 사진의 이미지 키(없으면 null)를 보내고, 만들어진 건의 id 를 돌려준다
    // 서버가 거절하면 서버 오류를 그대로, 연결 문제는 IOException 으로 실패한다
    suspend fun send(content: String, imageKey: String?): Result<Long>
}
