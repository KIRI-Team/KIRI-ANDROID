package com.kiri.data.image

import android.content.Context
import android.net.Uri
import com.kiri.domain.model.image.ImageContentType
import com.kiri.domain.model.image.UploadImage
import com.kiri.domain.repository.ImageUploadRepository
import com.kiri.domain.repository.ReviewPhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReviewPhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val images: ImageUploadRepository,
) : ReviewPhotoRepository {
    override suspend fun upload(uri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = Uri.parse(uri)
            require(source.scheme == "content") { "사진을 다시 선택해주세요." }
            val type = requireNotNull(ImageContentType.fromMime(context.contentResolver.getType(source))) {
                "JPEG, PNG, WebP 사진만 올릴 수 있어요."
            }
            val bytes = requireNotNull(context.contentResolver.openInputStream(source)).use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = stream.read(buffer)
                    if (size < 0) break
                    require(output.size().toLong() + size <= UploadImage.MAX_UPLOAD_IMAGE_BYTES) { "10MB 이하의 사진만 올릴 수 있어요." }
                    output.write(buffer, 0, size)
                }
                output.toByteArray()
            }
            images.upload(UploadImage(bytes, type))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewPhotoModule {
    @Binds internal abstract fun bindReviewPhotos(impl: ReviewPhotoRepositoryImpl): ReviewPhotoRepository
}
