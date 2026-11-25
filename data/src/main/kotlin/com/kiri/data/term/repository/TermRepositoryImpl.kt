package com.kiri.data.term.repository

import com.kiri.data.common.network.apiCommand
import com.kiri.data.common.network.apiResult
import com.kiri.domain.term.model.Term
import com.kiri.domain.term.repository.TermRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.term.remote.TermAgreeRequest
import com.kiri.data.term.remote.TermAgreement
import com.kiri.data.term.remote.TermApi
import com.kiri.data.term.remote.toTerm

internal class TermRepositoryImpl @Inject constructor(
    private val api: TermApi,
    private val json: Json,
) : TermRepository {

    override suspend fun getTerms(): Result<List<Term>> =
        apiResult(json) { api.getTerms() }.map { terms -> terms.map { it.toTerm() } }

    override suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit> =
        apiCommand(json) {
            api.agree(TermAgreeRequest(agreements.map { (id, agreed) -> TermAgreement(termId = id, isAgreed = agreed) }))
        }
}
