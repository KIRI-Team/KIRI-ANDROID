package com.kiri.data.bucket

import com.kiri.domain.model.bucket.BucketItem
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.model.bucket.BucketItemStatus
import com.kiri.domain.model.bucket.BucketSaver
import com.kiri.domain.repository.BucketItemRepository
import java.time.LocalDateTime

/** 테스트·미리보기용 가족 버킷리스트. 실제 서버 데이터를 대신하지 않는다.
 *  memberId 로 거르고, 최신 저장순(id 큰 순)으로 cursor(직전 페이지 마지막 id) 다음부터 size 개씩 돌려준다 */
class FakeBucketItemRepository(
    var savers: List<BucketSaver> = DemoSavers,
    var items: List<BucketItem> = DemoItems,
    // 정해 두면 그 오류로 실패한다
    var failure: Throwable? = null,
) : BucketItemRepository {
    val requests = mutableListOf<Triple<Long?, Long?, Int?>>()
    val deleted = mutableListOf<Long>()

    // 정해 두면 삭제가 그 오류로 실패한다. 성공하면 항목을 빼고 저장한 사람의 개수를 줄인다
    var deleteFailure: Throwable? = null

    override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> {
        deleteFailure?.let { return Result.failure(it) }
        val item = items.find { it.id == bucketItemId } ?: return Result.failure(com.kiri.domain.model.bucket.BucketItemNotFoundException())
        deleted += bucketItemId
        items = items - item
        savers = savers.map { if (it.memberId == item.saverId) it.copy(bucketCount = it.bucketCount - 1) else it }
        return Result.success(Unit)
    }

    override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?): Result<BucketItemPage> {
        requests += Triple(memberId, cursor, size)
        failure?.let { return Result.failure(it) }
        val filtered = items
            .filter { memberId == null || it.saverId == memberId }
            .sortedByDescending { it.id }
            .filter { cursor == null || it.id < cursor }
        val page = filtered.take(size ?: 20)
        val hasNext = filtered.size > page.size
        return Result.success(BucketItemPage(savers, page, nextCursor = page.lastOrNull()?.id?.takeIf { hasNext }, hasNext = hasNext))
    }

    companion object {
        val DemoSavers = listOf(BucketSaver(1, "엄마", 1), BucketSaver(2, "아빠", 2))

        fun item(
            id: Long,
            saverId: Long,
            saverNickname: String,
            status: BucketItemStatus = BucketItemStatus.SAVED,
            title: String = "버킷 $id",
            schedulable: Boolean = status == BucketItemStatus.SAVED,
            deletable: Boolean = false,
            scheduleId: Long? = null,
        ) = BucketItem(
            id = id,
            status = status,
            savedAt = LocalDateTime.of(2026, 10, 1, 10, 0).plusDays(id),
            missionId = id * 10,
            title = title,
            imageUrl = null,
            missionStatus = "ACTIVE",
            saverId = saverId,
            saverNickname = saverNickname,
            saverWithdrawn = false,
            schedulable = schedulable,
            deletable = deletable,
            scheduleId = scheduleId,
            scheduledDate = null,
        )

        val DemoItems = listOf(
            item(12, 2, "아빠", title = "광장시장에서 시장음식 먹어보기", deletable = true),
            item(11, 1, "엄마", BucketItemStatus.COMPLETED, title = "한강 뚜벅뚜벅 축제 가보기"),
            item(10, 2, "아빠", BucketItemStatus.SCHEDULED, title = "남산 둘레길 걷기"),
        )
    }
}
