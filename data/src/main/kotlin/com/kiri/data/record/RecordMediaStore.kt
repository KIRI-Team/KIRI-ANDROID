package com.kiri.data.record

import android.content.Context
import android.net.Uri
import com.kiri.domain.record.RecordPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RecordMediaStore {
    suspend fun retain(scope: String, photos: List<RecordPhoto>): List<RecordPhoto>

    suspend fun prune(scope: String, photos: List<RecordPhoto>) {}
}

object PassthroughRecordMediaStore : RecordMediaStore {
    override suspend fun retain(scope: String, photos: List<RecordPhoto>) = photos
}

/** Keep an app-private copy so provider grants cannot invalidate an already-saved review. */
@Singleton
class DeviceRecordMediaStore @Inject constructor(@ApplicationContext private val context: Context) :
    RecordMediaStore {
    override suspend fun prune(scope: String, photos: List<RecordPhoto>) =
        withContext(Dispatchers.IO) {
            require(scope.matches(Regex("[a-f0-9]{64}")))
            val directory = File(context.filesDir, "record-media/$scope")
            val retained =
                photos
                    .mapNotNull { photo ->
                        val uri = Uri.parse(photo.source)
                        if (uri.scheme == "file") uri.path else null
                    }
                    .toSet()
            directory
                .listFiles()
                .orEmpty()
                .filter { it.extension == "image" && it.absolutePath !in retained }
                .forEach { it.delete() }
        }

    override suspend fun retain(scope: String, photos: List<RecordPhoto>): List<RecordPhoto> =
        withContext(Dispatchers.IO) {
            require(scope.matches(Regex("[a-f0-9]{64}"))) { "사진 보관 영역을 확인해주세요." }
            val directory =
                File(context.filesDir, "record-media/$scope").apply {
                    if (!exists() && !mkdirs()) throw IOException("사진 보관 폴더를 만들지 못했어요.")
                }
            photos.map { photo ->
                val uri = Uri.parse(photo.source)
                val root = File(context.filesDir, "record-media").canonicalFile
                if (
                    uri.scheme == "file" &&
                        File(requireNotNull(uri.path)).canonicalFile.parentFile ==
                            directory.canonicalFile
                ) {
                    check(File(requireNotNull(uri.path)).isFile) { "저장된 사진을 찾을 수 없어요." }
                    photo
                } else {
                    require(uri.scheme == "content") { "이 사진을 저장할 수 없어요." }
                    val name =
                        MessageDigest.getInstance("SHA-256")
                            .digest(photo.source.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                    val target = File(directory, "$name.image")
                    check(target.canonicalFile.toPath().startsWith(root.toPath()))
                    if (!target.exists()) {
                        val pending = File.createTempFile("photo-", ".pending", directory)
                        try {
                            val input =
                                context.contentResolver.openInputStream(uri)
                                    ?: throw IOException("사진을 읽지 못했어요.")
                            input.use { source ->
                                pending.outputStream().use { output ->
                                    source.copyTo(output)
                                    output.fd.sync()
                                }
                            }
                            if (pending.length() == 0L || !pending.renameTo(target))
                                throw IOException("사진을 보관하지 못했어요.")
                        } finally {
                            pending.delete()
                        }
                    }
                    photo.copy(source = Uri.fromFile(target).toString())
                }
            }
        }
}
