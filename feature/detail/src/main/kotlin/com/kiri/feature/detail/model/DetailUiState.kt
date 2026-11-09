package com.kiri.feature.detail.model

import com.kiri.domain.detail.*

data class DetailUiState(
    val loading: Boolean = true,
    val detail: MissionDetail? = null,
    val draft: Set<Condition> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
    val memoriesSelected: Boolean = false,
    val memoryDraft: MemoryDraft = MemoryDraft(),
    val submittedMemory: MemoryDraft? = null,
    // 서버가 이미 취향·컨디션으로 정렬한 근처 놀거리면 true. 앱에서 다시 정렬하지 않는다
    val nearbyRankedByServer: Boolean = false,
) {
    val nearby: List<NearbyPlace>
        get() = detail?.let { if (nearbyRankedByServer) it.nearby else rankNearby(it.nearby, it.conditions) }.orEmpty()
}
