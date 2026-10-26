package com.kiri.domain.repository

import com.kiri.domain.model.term.Term

interface TermRepository {
    // 동의를 받아야 하는 약관 목록
    suspend fun getTerms(): Result<List<Term>>

    // 약관마다 동의 여부를 보낸다. key 는 약관 id, value 는 동의 여부 (선택 약관 미동의도 false 로 보낸다)
    suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit>
}
