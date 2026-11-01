package com.kiri.data.taste

import com.kiri.data.network.apiResult
import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.repository.TasteRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class TasteRepositoryImpl @Inject constructor(
    private val api: TasteApi,
    private val json: Json,
) : TasteRepository {

    override suspend fun getMyTaste(): Result<MyTaste> =
        apiResult(json) { api.getMyTaste() }.map { it.toMyTaste() }

    override suspend fun saveMyTaste(taste: Taste): Result<MyTaste> =
        apiResult(json) { api.saveMyTaste(taste.toRequest()) }.map { it.toMyTaste() }
}
