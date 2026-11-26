package com.kiri.feature.record.presentation.bucket

import com.kiri.domain.bucket.model.BucketItem

// 버킷리스트 카드 한 장. 같은 미션을 여러 가족이 저장했으면 한 장으로 합쳐 "엄마, 아빠가 저장했어요!" 처럼 보여준다
internal data class BucketCardUi(
    // 카드를 눌렀을 때 일정 잡기·삭제하기를 적용할 항목
    val item: BucketItem,
    // 저장한 가족. 먼저 저장한 순서다
    val saverNames: List<String>,
) {
    val saverLabel: String get() = saverNames.joinToString(", ")
}

// 같은 미션이고 상태와 연결된 일정까지 같은 항목을 한 카드로 합친다.
// 상태가 다르면(예: 한 사람 것은 일정이 잡혔고 다른 사람 것은 저장 상태) 할 수 있는 일이 달라 따로 보여준다.
// 카드 순서는 서버 순서(최신 저장순)에서 그 미션이 처음 나온 자리다.
// 대표 항목은 내가 지울 수 있는(내가 저장한) 항목, 없으면 가장 최근에 저장된 항목이다
internal fun List<BucketItem>.mergedByMission(): List<BucketCardUi> =
    groupBy { Triple(it.missionId, it.status, it.scheduleId) }.values.map { group ->
        BucketCardUi(
            item = group.firstOrNull { it.deletable } ?: group.first(),
            saverNames = group
                .sortedWith(compareBy(nullsLast(naturalOrder())) { it.savedAt })
                .map { it.saverNickname }
                .distinct(),
        )
    }

// 이 항목이 들어간 카드의 저장한 가족 이름. 일정 잡기 시트·화면의 "OO, XX가 저장했어요!" 에 쓴다
internal fun List<BucketItem>.saverLabelOf(item: BucketItem): String =
    mergedByMission().firstOrNull { card -> card.item.id == item.id }?.saverLabel ?: item.saverNickname
