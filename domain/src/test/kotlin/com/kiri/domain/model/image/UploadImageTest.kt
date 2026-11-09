package com.kiri.domain.model.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 서버가 받는 형식과 크기(1바이트 ~ 10MB)만 올릴 수 있다
class UploadImageTest {
    @Test
    fun 비어_있지_않고_10MB_이하인_이미지만_올릴_수_있다() {
        val max = UploadImage.MAX_UPLOAD_IMAGE_BYTES.toInt()
        assertFalse(UploadImage(ByteArray(0), ImageContentType.JPEG).isUploadable)
        assertTrue(UploadImage(ByteArray(1), ImageContentType.JPEG).isUploadable)
        assertTrue(UploadImage(ByteArray(max), ImageContentType.JPEG).isUploadable)
        assertFalse(UploadImage(ByteArray(max + 1), ImageContentType.JPEG).isUploadable)
    }

    @Test
    fun 기기가_알려준_MIME_타입으로_지원_형식을_찾는다() {
        assertEquals(ImageContentType.JPEG, ImageContentType.fromMime("image/jpeg"))
        assertEquals(ImageContentType.PNG, ImageContentType.fromMime("IMAGE/PNG"))
        assertEquals(ImageContentType.WEBP, ImageContentType.fromMime("image/webp"))
        assertNull(ImageContentType.fromMime("image/heic"))
        assertNull(ImageContentType.fromMime(null))
    }
}
