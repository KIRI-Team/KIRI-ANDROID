package com.kiri.data.image

import com.kiri.data.network.apiResult
import com.kiri.domain.model.image.ImageUploadFailedException
import com.kiri.domain.model.image.InvalidUploadImageException
import com.kiri.domain.model.image.UploadImage
import com.kiri.domain.repository.ImageUploadRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// 업로드 주소를 발급받은 뒤 그 주소로 이미지를 PUT 하고, 성공하면 이미지 키를 돌려준다
// 저장소(S3)에는 토큰을 보내지 않으므로 토큰 처리가 없는 업로드 전용 클라이언트로 올린다
internal class ImageUploadRepositoryImpl @Inject constructor(
    private val api: ImageApi,
    @ImageUploadClient private val uploadClient: OkHttpClient,
    private val json: Json,
) : ImageUploadRepository {

    override suspend fun upload(image: UploadImage): Result<String> {
        if (!image.isUploadable) return Result.failure(InvalidUploadImageException())
        val mime = image.contentType.mime
        val issued = apiResult(json) { api.issuePresignedUrl(PresignedUrlRequest(mime, image.size)) }
            .getOrElse { return Result.failure(it) }
        return try {
            put(issued.presignedUrl, image.bytes, mime)
            Result.success(issued.imageKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Content-Type 은 발급 요청과 같은 문자열, 크기는 발급 요청과 같은 바이트 수로 보낸다
    private suspend fun put(url: String, bytes: ByteArray, mime: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .put(bytes.toRequestBody(mime.toMediaType()))
            .build()
        uploadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ImageUploadFailedException(response.code)
        }
    }
}
