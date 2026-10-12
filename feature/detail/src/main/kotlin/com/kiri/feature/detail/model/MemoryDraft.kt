package com.kiri.feature.detail.model

/** A screen-local preview. It is never sent to, or persisted by, a record repository. */
data class MemoryDraft(
    val rating: Int = 0,
    val photos: List<String> = emptyList(),
    val text: String = "",
    val repeatActivity: Boolean? = null,
) {
    val canSubmit: Boolean get() = rating in 1..5 && photos.isNotEmpty() && text.isNotBlank() && text.length <= 500 && repeatActivity != null
}
