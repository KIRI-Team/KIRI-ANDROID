package com.kiri.uikit.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCacheKeyTest {
    @Test fun signedUrlsOfSamePhotoShareCacheKey() {
        val first = imageCacheKey("https://cdn.kiri.app/reviews/1.jpg?X-Amz-Date=20261119T000000Z&X-Amz-Signature=aaa")
        val second = imageCacheKey("https://cdn.kiri.app/reviews/1.jpg?X-Amz-Date=20261119T010000Z&X-Amz-Signature=bbb")
        assertEquals("https://cdn.kiri.app/reviews/1.jpg", first)
        assertEquals(first, second)
    }

    @Test fun fragmentIsRemovedFromServerUrl() {
        assertEquals("http://cdn.kiri.app/a.png", imageCacheKey("http://cdn.kiri.app/a.png#top"))
    }

    @Test fun urlWithoutQueryStaysSame() {
        assertEquals("https://cdn.kiri.app/a.png", imageCacheKey("https://cdn.kiri.app/a.png"))
    }

    @Test fun devicePhotoKeepsQuery() {
        val uri = "content://media/picker/0/com.android.providers.media.photopicker/media/1000?mode=r"
        assertEquals(uri, imageCacheKey(uri))
    }
}
