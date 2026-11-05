package com.kiri.data.bucket

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.bucket.BucketItemNotFoundException
import com.kiri.domain.model.bucket.CompletedBucketItemException
import com.kiri.domain.model.bucket.NotBucketItemOwnerException
import com.kiri.domain.model.bucket.ScheduledBucketItemException
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.repository.BucketItemRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class BucketItemRepositoryImpl @Inject constructor(
    private val api: BucketItemApi,
    private val json: Json,
) : BucketItemRepository {

    // 가족 공간이 없으면(둘러보기) 보여줄 버킷이 없는 것이므로 오류 대신 빈 페이지로 본다
    override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?): Result<BucketItemPage> =
        apiResult(json) { api.getBucketItems(memberId, cursor, size) }
            .map { it.toPage() }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == NO_FAMILY) BucketItemPage.Empty else throw error
            }

    override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> =
        apiCommand(json) { api.deleteBucketItem(bucketItemId) }
            .recoverCatching { error ->
                throw when ((error as? KiriApiException)?.code) {
                    NOT_FOUND -> BucketItemNotFoundException()
                    NOT_OWNER -> NotBucketItemOwnerException()
                    SCHEDULED -> ScheduledBucketItemException()
                    COMPLETED -> CompletedBucketItemException()
                    else -> error
                }
            }

    private companion object {
        const val NO_FAMILY = "BUCKET403_1"
        const val NOT_OWNER = "BUCKET403_2"
        const val NOT_FOUND = "BUCKET404_1"
        const val SCHEDULED = "BUCKET409_3"
        const val COMPLETED = "BUCKET409_4"
    }
}
