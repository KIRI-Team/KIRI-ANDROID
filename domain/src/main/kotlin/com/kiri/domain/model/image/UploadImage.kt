package com.kiri.domain.model.image

// 서버가 받는 이미지 형식
enum class ImageContentType(val mime: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    companion object {
        // 기기가 알려준 MIME 타입으로 찾는다. 지원하지 않는 형식이면 null
        fun fromMime(mime: String?): ImageContentType? = entries.firstOrNull { it.mime.equals(mime, ignoreCase = true) }
    }
}

// 올릴 이미지. 압축·리사이징은 만들기 전에 끝내야 한다 (발급 요청의 크기와 올리는 크기가 정확히 같아야 한다)
class UploadImage(val bytes: ByteArray, val contentType: ImageContentType) {
    val size: Long get() = bytes.size.toLong()

    // 비어 있지 않고 10MB 이하여야 올릴 수 있다
    val isUploadable: Boolean get() = size in 1..MAX_UPLOAD_IMAGE_BYTES

    companion object {
        const val MAX_UPLOAD_IMAGE_BYTES: Long = 10L * 1024 * 1024
    }
}

// 비어 있거나 10MB 를 넘는 이미지
class InvalidUploadImageException : Exception("10MB 이하의 사진만 올릴 수 있어요.")

// 발급받은 주소로 올리지 못했다 (주소 만료, 헤더·크기 불일치 등). statusCode 는 저장소가 돌려준 HTTP 상태
class ImageUploadFailedException(val statusCode: Int) : Exception("사진을 올리지 못했어요.")
