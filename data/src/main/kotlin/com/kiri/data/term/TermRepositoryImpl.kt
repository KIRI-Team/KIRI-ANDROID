package com.kiri.data.term

import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.term.Term
import com.kiri.domain.repository.TermRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class TermRepositoryImpl @Inject constructor(
    private val api: TermApi,
    private val json: Json,
) : TermRepository {

    override suspend fun getTerms(): Result<List<Term>> =
        apiResult(json) { api.getTerms() }.map { terms ->
            terms.map { Term(id = it.termId, title = it.title, content = it.description, required = it.isRequired) }
        }

    override suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit> =
        apiCommand(json) {
            api.agree(TermAgreeRequest(agreements.map { (id, agreed) -> TermAgreement(termId = id, isAgreed = agreed) }))
        }
}
