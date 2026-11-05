package com.kiri.feature.record.presentation.bucket

import com.kiri.domain.model.bucket.BucketItem
import com.kiri.domain.model.bucket.BucketItemStatus

// 버킷 카드를 눌렀을 때 뜨는 일정 잡기·삭제하기 시트의 버튼 상태
internal data class BucketOptions(
    val scheduleLabel: String,
    val scheduleEnabled: Boolean,
    // 일정이 잡힌 버킷이면 연결된 서버 일정 id. 누르면 그 일정의 상세로 간다
    val linkedScheduleId: Long?,
    val deleteEnabled: Boolean,
)

// 일정 잡기는 서버가 schedulable 로 허락한 버킷만, 삭제는 deletable 인 버킷만 누를 수 있다.
// 일정이 잡힌 버킷은 연결된 일정이 있을 때만 그 일정을 열 수 있다 (일정 기능이 연결되기 전까지 서버가 id 를 주지 않는다)
internal fun BucketItem.options(): BucketOptions = when (status) {
    BucketItemStatus.COMPLETED -> BucketOptions("완료한 활동", scheduleEnabled = false, linkedScheduleId = null, deleteEnabled = deletable)
    BucketItemStatus.SCHEDULED -> BucketOptions("연결된 약속 보기", scheduleEnabled = scheduleId != null, linkedScheduleId = scheduleId, deleteEnabled = deletable)
    else -> BucketOptions("일정 잡기", scheduleEnabled = schedulable, linkedScheduleId = null, deleteEnabled = deletable)
}
