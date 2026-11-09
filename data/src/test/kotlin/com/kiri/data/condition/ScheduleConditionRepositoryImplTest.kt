package com.kiri.data.condition

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.detail.Condition
import com.kiri.domain.model.condition.ConditionCustom
import com.kiri.domain.model.condition.ConditionCustomUnavailableException
import com.kiri.domain.model.condition.NotFinalParticipantException
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.mission.WalkingLevel
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// 컨디션 커스텀 조회 API 응답을 앱의 조건으로 바꾸고, 실패 코드를 도메인 오류로 바꾸는지 확인한다
class ScheduleConditionRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): ScheduleConditionRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ScheduleConditionApi::class.java)
        return ScheduleConditionRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun success(result: String) =
        """{"isSuccess":true,"code":"SCHEDULE200_6","message":"컨디션 커스텀 조회 성공","result":$result}"""

    private fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"실패","result":null}"""

    @Test
    fun 일정_경로로_GET_하고_미션_정보와_적용된_조건을_돌려준다() = runTest {
        enqueue(
            200,
            success(
                """{"missionTitle":"한강 뚜벅뚜벅 축제 가보기","walkingLevel":"MEDIUM","durationMinMinutes":60,""" +
                    """"durationMaxMinutes":180,"conditions":["REDUCE_STEPS","MORE_REST"]}""",
            ),
        )

        val custom = repository().getConditionCustom(7).getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/7/condition", request.url.encodedPath)
        assertEquals(
            ConditionCustom(
                "한강 뚜벅뚜벅 축제 가보기", WalkingLevel.MEDIUM, 60, 180,
                setOf(Condition.LESS_WALKING, Condition.MORE_REST),
            ),
            custom,
        )
    }

    @Test
    fun 서버_조건_이름을_모두_앱의_조건으로_바꾸고_모르는_값은_뺀다() = runTest {
        enqueue(
            200,
            success(
                """{"missionTitle":"산책","walkingLevel":null,"conditions":""" +
                    """["REDUCE_STEPS","MORE_REST","LOWER_BUDGET","AVOID_CROWD","NEW_ONE"]}""",
            ),
        )

        val custom = repository().getConditionCustom(1).getOrThrow()

        assertEquals(Condition.entries.toSet(), custom.conditions)
        assertNull(custom.walkingLevel)
        assertNull(custom.durationMinMinutes)
    }

    @Test
    fun 적용한_조건이_없으면_빈_집합이다() = runTest {
        enqueue(200, success("""{"missionTitle":"산책","walkingLevel":"LOW","conditions":[]}"""))

        assertTrue(repository().getConditionCustom(1).getOrThrow().conditions.isEmpty())
    }

    private val applied = """{"isSuccess":true,"code":"SCHEDULE200_7","message":"컨디션 커스텀 적용 성공","result":null}"""

    @Test
    fun 같은_경로로_PUT_하고_고른_조건을_서버_이름으로_보낸다() = runTest {
        enqueue(200, applied)

        repository().applyConditions(7, setOf(Condition.MORE_REST, Condition.LESS_WALKING)).getOrThrow()

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/schedules/7/condition", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        // 명세 예시와 같은 {"conditions": ["REDUCE_STEPS", "MORE_REST"]}
        assertEquals(listOf("REDUCE_STEPS", "MORE_REST"), body["conditions"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun 모든_조건을_해제하면_빈_배열을_빠뜨리지_않고_보낸다() = runTest {
        enqueue(200, applied)
        enqueue(200, applied)

        repository().applyConditions(7, emptySet()).getOrThrow()
        repository().applyConditions(7, Condition.entries.toSet()).getOrThrow()

        // conditions 를 빼면 400 이라 빈 배열도 보낸다
        assertEquals("""{"conditions":[]}""", server.takeRequest().body!!.utf8())
        val all = kiriJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals(
            listOf("REDUCE_STEPS", "MORE_REST", "LOWER_BUDGET", "AVOID_CROWD"),
            all["conditions"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun 적용_실패_코드도_도메인_오류로_바꾼다() = runTest {
        enqueue(409, failure("SCHEDULE409_5"))
        enqueue(403, failure("SCHEDULE403_5"))
        enqueue(400, failure("COMMON400_1"))

        val repo = repository()
        assertTrue(repo.applyConditions(1, emptySet()).exceptionOrNull() is ConditionCustomUnavailableException)
        assertTrue(repo.applyConditions(1, emptySet()).exceptionOrNull() is NotFinalParticipantException)
        assertEquals("COMMON400_1", (repo.applyConditions(1, emptySet()).exceptionOrNull() as KiriApiException).code)
    }

    @Test
    fun 실패_코드를_도메인_오류로_바꾸고_나머지는_그대로_돌려준다() = runTest {
        enqueue(403, failure("SCHEDULE403_1"))
        enqueue(403, failure("SCHEDULE403_5"))
        enqueue(404, failure("SCHEDULE404_2"))
        enqueue(409, failure("SCHEDULE409_6"))
        enqueue(404, failure("MEMBER404_1"))

        val repo = repository()
        assertTrue(repo.getConditionCustom(1).exceptionOrNull() is NoFamilyException)
        assertTrue(repo.getConditionCustom(1).exceptionOrNull() is NotFinalParticipantException)
        assertTrue(repo.getConditionCustom(1).exceptionOrNull() is ScheduleNotFoundException)
        assertTrue(repo.getConditionCustom(1).exceptionOrNull() is ConditionCustomUnavailableException)
        assertEquals("MEMBER404_1", (repo.getConditionCustom(1).exceptionOrNull() as KiriApiException).code)
    }
}
