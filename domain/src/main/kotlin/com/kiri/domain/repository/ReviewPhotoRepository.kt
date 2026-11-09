package com.kiri.domain.repository

interface ReviewPhotoRepository {
    suspend fun upload(uri: String): Result<String>
}
