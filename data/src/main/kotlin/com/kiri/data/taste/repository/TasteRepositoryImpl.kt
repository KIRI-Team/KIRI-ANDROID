package com.kiri.data.taste.repository

import com.kiri.data.common.network.apiResult
import com.kiri.domain.taste.model.MyTaste
import com.kiri.domain.taste.model.Taste
import com.kiri.domain.taste.repository.TasteRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.taste.remote.TasteApi
import com.kiri.data.taste.remote.toMyTaste
import com.kiri.data.taste.remote.toRequest

internal class TasteRepositoryImpl @Inject constructor(
    private val api: TasteApi,
    private val json: Json,
) : TasteRepository {

    override suspend fun getMyTaste(): Result<MyTaste> =
        apiResult(json) { api.getMyTaste() }.map { it.toMyTaste() }

    override suspend fun saveMyTaste(taste: Taste): Result<MyTaste> =
        apiResult(json) { api.saveMyTaste(taste.toRequest()) }.map { it.toMyTaste() }
}
