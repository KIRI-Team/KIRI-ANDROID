package com.kiri.domain.model.bucket

import java.time.LocalDate
import java.time.LocalDateTime

// 가족 버킷리스트 한 페이지. 최신 저장순이다
data class BucketItemPage(
    // 필터 칩에 쓰는 현재 가족 구성원. 저장 개수는 필터·페이지와 상관없이 가족 전체 기준이다 (탈퇴한 회원은 없다)
    val savers: List<BucketSaver>,
    val items: List<BucketItem>,
    // 다음 페이지를 부를 때 넣는 값. hasNext 가 false 면 마지막 페이지다
    val nextCursor: Long?,
    val hasNext: Boolean,
) {
    companion object {
        val Empty = BucketItemPage(emptyList(), emptyList(), nextCursor = null, hasNext = false)
    }
}

data class BucketSaver(
    val memberId: Long,
    val nickname: String,
    val bucketCount: Int,
)

data class BucketItem(
    val id: Long,
    // 앱이 모르는 값이면 null
    val status: BucketItemStatus?,
    // 읽지 못하면 null
    val savedAt: LocalDateTime?,
    val missionId: Long,
    val title: String,
    val imageUrl: String?,
    // 미션 상태. 종료·취소·비공개가 되어도 항목은 남는다 (ACTIVE, ENDED 등 서버 값 그대로)
    val missionStatus: String?,
    val saverId: Long,
    // 탈퇴한 회원이면 "탈퇴한 사용자"
    val saverNickname: String,
    val saverWithdrawn: Boolean,
    // 저장 상태이고 미션이 공개 중이며 기간이 끝나지 않았을 때만 true
    val schedulable: Boolean,
    // 내가 저장했고 저장 상태일 때만 true
    val deletable: Boolean,
    // 일정 기능이 연결되기 전까지 null
    val scheduleId: Long?,
    val scheduledDate: LocalDate?,
)

enum class BucketItemStatus { SAVED, SCHEDULED, COMPLETED }

// 버킷 삭제 실패. 없거나 다른 가족의 항목이다
class BucketItemNotFoundException : Exception("버킷 항목을 찾을 수 없습니다.")

// 본인이 저장한 항목이 아니다 (저장한 회원이 탈퇴한 항목도 같다)
class NotBucketItemOwnerException : Exception("본인이 저장한 버킷 항목만 삭제할 수 있습니다.")

// 일정이 잡힌 항목은 일정을 취소한 뒤 삭제할 수 있다
class ScheduledBucketItemException : Exception("일정이 잡힌 버킷 항목입니다.")

// 완료된 항목은 완료 기록으로 남기므로 삭제할 수 없다
class CompletedBucketItemException : Exception("완료된 버킷 항목입니다.")
