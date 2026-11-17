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

// 미션이 종료·취소·비공개로 바뀐 버킷. 서버는 이런 항목도 그대로 주고 mission.status 로 구분하게 한다
internal val BucketItem.missionEnded: Boolean
    get() = missionStatus != null && missionStatus != ACTIVE_MISSION_STATUS

// 일정 잡기는 서버가 schedulable 로 허락한 버킷만, 삭제는 deletable 인 버킷만 누를 수 있다.
// 연결된 약속 보기는 연결된 활성 일정이 있고 미션이 종료되지 않았을 때만 누를 수 있다. 그 밖에는 비활성으로 둔다
internal fun BucketItem.options(): BucketOptions = when (status) {
    BucketItemStatus.COMPLETED -> BucketOptions("완료한 활동", scheduleEnabled = false, linkedScheduleId = null, deleteEnabled = deletable)
    BucketItemStatus.SCHEDULED -> {
        val linked = scheduleId?.takeIf { !missionEnded }
        BucketOptions("연결된 약속 보기", scheduleEnabled = linked != null, linkedScheduleId = linked, deleteEnabled = deletable)
    }
    else -> BucketOptions("일정 잡기", scheduleEnabled = schedulable, linkedScheduleId = null, deleteEnabled = deletable)
}

// 버킷 카드 위쪽 배지 (저장한 사람 배지 앞에 붙는다)
internal fun BucketItem.statusBadges(): List<String> = when {
    status == BucketItemStatus.COMPLETED -> listOf("완료된 일정")
    missionEnded -> listOf("종료된 미션")
    else -> emptyList()
}

private const val ACTIVE_MISSION_STATUS = "ACTIVE"
