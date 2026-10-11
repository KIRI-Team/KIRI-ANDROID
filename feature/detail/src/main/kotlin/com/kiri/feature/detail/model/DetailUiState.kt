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
) {
    val nearby: List<NearbyPlace> get() = detail?.let { rankNearby(it.nearby, it.conditions) }.orEmpty()
}
