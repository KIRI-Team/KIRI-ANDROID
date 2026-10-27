package com.kiri.data.local

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.data.record.DeviceRecordMediaStore
import com.kiri.domain.record.RecordPhoto
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordMediaStoreTest {
    @Test
    fun copiedPhotoSurvivesProviderDeletionAndCannotBeReusedAcrossAccounts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val scope =
            MessageDigest.getInstance("SHA-256")
                .digest(UUID.randomUUID().toString().toByteArray())
                .joinToString("") { "%02x".format(it) }
        val otherScope =
            MessageDigest.getInstance("SHA-256")
                .digest(UUID.randomUUID().toString().toByteArray())
                .joinToString("") { "%02x".format(it) }
        val source =
            requireNotNull(
                resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "kiri-test-${UUID.randomUUID()}.png",
                        )
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    },
                )
            )
        var saved: File? = null
        var sourceDeleted = false
        try {
            val bitmap =
                Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(android.graphics.Color.GREEN)
                }
            requireNotNull(resolver.openOutputStream(source)).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
            val photo =
                DeviceRecordMediaStore(context)
                    .retain(scope, listOf(RecordPhoto("photo", source.toString())))
                    .single()
            saved = File(requireNotNull(Uri.parse(photo.source).path))
            assertEquals(1, resolver.delete(source, null, null))
            sourceDeleted = true
            assertTrue(saved.isFile)
            assertEquals(
                android.graphics.Color.GREEN,
                BitmapFactory.decodeFile(saved.path).getPixel(0, 0),
            )
            assertEquals(
                photo,
                DeviceRecordMediaStore(context).retain(scope, listOf(photo)).single(),
            )
            assertTrue(
                runCatching { DeviceRecordMediaStore(context).retain(otherScope, listOf(photo)) }
                    .isFailure
            )
            assertTrue(
                runCatching { DeviceRecordMediaStore(context).retain("../escape", listOf(photo)) }
                    .isFailure
            )
            DeviceRecordMediaStore(context).prune(scope, listOf(photo))
            assertTrue(saved.isFile)
            DeviceRecordMediaStore(context).prune(otherScope, emptyList())
            assertTrue(saved.isFile)
            DeviceRecordMediaStore(context).prune(scope, emptyList())
            assertFalse(saved.exists())
        } finally {
            if (!sourceDeleted) resolver.delete(source, null, null)
            saved?.delete()
            File(context.filesDir, "record-media/$scope").delete()
            File(context.filesDir, "record-media/$otherScope").delete()
        }
    }
}
