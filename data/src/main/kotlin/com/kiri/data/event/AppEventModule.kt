package com.kiri.data.event

import com.kiri.data.network.ApiResponse
import com.kiri.data.network.apiCommand
import com.kiri.domain.repository.AppEventRepository
import com.kiri.domain.time.kiriNow
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AppEventApi {
    @POST("api/v1/events")
    suspend fun record(@Body batch: AppEventBatch): ApiResponse<AppEventResponse>
}

@Serializable
internal data class AppEventBatch(val events: List<AppEventRequest>)

@Serializable
internal data class AppEventRequest(
    val eventId: String,
    val eventName: String,
    val properties: Map<String, Long>,
    val platform: String,
    val occurredAt: String,
)

@Serializable
internal data class AppEventResponse(val inserted: Int)

internal class AppEventRepositoryImpl(
    private val api: AppEventApi,
    private val json: Json,
    private val clock: Clock,
) : AppEventRepository {
    override suspend fun recordMissionDetailView(missionId: Long): Result<Unit> {
        val event = AppEventRequest(
            eventId = UUID.randomUUID().toString(),
            eventName = "mission_detail_view",
            properties = mapOf("missionId" to missionId),
            platform = "ANDROID",
            occurredAt = clock.kiriNow().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
        return apiCommand(json) { api.record(AppEventBatch(listOf(event))) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppEventModule {
    @Provides
    fun provideAppEventRepository(retrofit: Retrofit, json: Json, clock: Clock): AppEventRepository =
        AppEventRepositoryImpl(retrofit.create(AppEventApi::class.java), json, clock)
}
