package com.kiri.domain.review.repository

interface ReviewPhotoRepository {
    suspend fun upload(uri: String): Result<String>
}
