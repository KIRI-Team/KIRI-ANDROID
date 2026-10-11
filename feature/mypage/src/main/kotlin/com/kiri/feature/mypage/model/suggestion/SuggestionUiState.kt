package com.kiri.feature.mypage.model.suggestion

data class SuggestionUiState(
    val content: String = "",
    // 고른 사진의 content Uri 문자열 (고른 순서대로)
    val photoUris: List<String> = emptyList(),
) {
    // 예: 0/500
    val contentCountText: String
        get() = "${content.length}/$MAX_CONTENT_LENGTH"

    val remainingPhotoCount: Int
        get() = (MAX_PHOTO_COUNT - photoUris.size).coerceAtLeast(0)

    val canAddPhoto: Boolean
        get() = remainingPhotoCount > 0

    // 내용이 있어야 보낼 수 있다 (사진은 선택)
    val canSend: Boolean
        get() = content.isNotBlank()

    companion object {
        const val MAX_CONTENT_LENGTH = 500
        const val MAX_PHOTO_COUNT = 5
    }
}
