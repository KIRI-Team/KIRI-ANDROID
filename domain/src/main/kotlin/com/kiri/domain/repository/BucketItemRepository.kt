package com.kiri.domain.repository

import com.kiri.domain.model.bucket.BucketItemPage

interface BucketItemRepository {
    // 가족 구성원들이 저장한 버킷을 최신 저장순으로 한 페이지 불러온다.
    // memberId 를 넣으면 그 구성원이 저장한 항목만, cursor 를 넣으면 그 다음 페이지를 불러온다.
    // 가족 공간이 없는 회원(둘러보기)이면 빈 페이지를 돌려준다
    suspend fun getBucketItems(memberId: Long? = null, cursor: Long? = null, size: Int? = null): Result<BucketItemPage>

    // 내가 저장한 버킷 항목을 삭제한다. 삭제한 뒤에는 같은 미션을 다시 저장할 수 있다
    // 없거나 다른 가족의 항목이면 BucketItemNotFoundException, 내가 저장하지 않았으면 NotBucketItemOwnerException,
    // 일정이 잡혔으면 ScheduledBucketItemException, 완료됐으면 CompletedBucketItemException 으로 실패한다
    suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit>
}
