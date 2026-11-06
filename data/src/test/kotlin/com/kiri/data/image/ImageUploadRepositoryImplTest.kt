package com.kiri.data.image

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.image.ImageContentType
import com.kiri.domain.model.image.ImageUploadFailedException
import com.kiri.domain.model.image.InvalidUploadImageException
import com.kiri.domain.model.image.UploadImage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// 업로드 주소를 발급받아 그 주소로 이미지를 PUT 하고, 성공하면 이미지 키를 돌려주는지 확인한다
class ImageUploadRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): ImageUploadRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ImageApi::class.java)
        return ImageUploadRepositoryImpl(api, OkHttpClient(), kiriJson)
    }

    private fun enqueue(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    // 업로드 주소는 같은 테스트 서버의 다른 경로로 준다
    private fun issued(key: String = "pending/user/1/a.jpg"): String {
        val url = server.url("/s3/$key?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc")
        return """{"isSuccess":true,"code":"IMAGE200_1","message":"Presigned URL 발급 성공",""" +
            """"result":{"presignedUrl":"$url","imageKey":"$key","expiresAt":"2026-10-29T14:35:00"}}"""
    }

    private val photo = UploadImage(ByteArray(1234) { it.toByte() }, ImageContentType.JPEG)

    @Test
    fun 형식과_크기로_주소를_발급받고_같은_헤더와_크기로_PUT_한_뒤_키를_돌려준다() = runTest {
        enqueue(200, issued())
        enqueue(200)

        val key = repository().upload(photo).getOrThrow()

        val issue = server.takeRequest()
        assertEquals("POST", issue.method)
        assertEquals("/api/v1/images/presigned-url", issue.url.encodedPath)
        val body = kiriJson.parseToJsonElement(issue.body!!.utf8()).jsonObject
        assertEquals("image/jpeg", body["contentType"]!!.jsonPrimitive.content)
        assertEquals(1234L, body["contentLength"]!!.jsonPrimitive.long)

        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/s3/pending/user/1/a.jpg", put.url.encodedPath)
        // 서명이 담긴 주소를 그대로 쓴다
        assertEquals("abc", put.url.queryParameter("X-Amz-Signature"))
        // Content-Type 은 발급 요청과 같은 문자열이어야 하고, 토큰은 보내지 않는다
        assertEquals("image/jpeg", put.headers["Content-Type"])
        assertNull(put.headers["Authorization"])
        assertEquals("1234", put.headers["Content-Length"])
        assertArrayEquals(photo.bytes, put.body!!.toByteArray())
        assertEquals("pending/user/1/a.jpg", key)
    }

    @Test
    fun 비어_있거나_10MB_를_넘는_이미지는_요청하지_않고_거절한다() = runTest {
        val empty = UploadImage(ByteArray(0), ImageContentType.PNG)
        val tooLarge = UploadImage(ByteArray((UploadImage.MAX_UPLOAD_IMAGE_BYTES + 1).toInt()), ImageContentType.WEBP)

        assertTrue(repository().upload(empty).exceptionOrNull() is InvalidUploadImageException)
        assertTrue(repository().upload(tooLarge).exceptionOrNull() is InvalidUploadImageException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun 저장소가_거절하면_업로드_실패로_보고_키를_돌려주지_않는다() = runTest {
        enqueue(200, issued())
        // 주소 만료나 헤더·크기 불일치면 S3 가 403 을 준다
        enqueue(403, "<Error><Code>SignatureDoesNotMatch</Code></Error>")

        val error = repository().upload(photo).exceptionOrNull()

        assertTrue(error is ImageUploadFailedException)
        assertEquals(403, (error as ImageUploadFailedException).statusCode)
    }

    @Test
    fun 발급이_거절되면_올리지_않고_서버_오류를_돌려준다() = runTest {
        enqueue(500, """{"isSuccess":false,"code":"IMAGE500_1","message":"이미지 저장소 처리 중 오류가 발생했습니다.","result":null}""")

        val error = repository().upload(photo).exceptionOrNull()

        assertEquals("IMAGE500_1", (error as KiriApiException).code)
        assertEquals(1, server.requestCount)
    }
}
