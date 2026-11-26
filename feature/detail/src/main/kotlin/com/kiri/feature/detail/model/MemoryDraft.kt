package com.kiri.feature.detail.model

/** Validated screen draft; the record repository enforces ownership and saves its mapped media. */
data class MemoryDraft(
    val rating: Int = 0,
    val photos: List<String> = emptyList(),
    val text: String = "",
    val repeatActivity: Boolean? = null,
) {
    val canSubmit: Boolean get() = rating in 1..5 && photos.isNotEmpty() && photos.all { it.isNotBlank() } && text.isNotBlank() && text.length <= 500 && repeatActivity != null
}
