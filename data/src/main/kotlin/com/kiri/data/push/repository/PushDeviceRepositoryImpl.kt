package com.kiri.data.push.repository

import com.kiri.data.common.network.apiCommand
import com.kiri.domain.push.repository.PushDeviceRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.push.local.DeviceIdStore
import com.kiri.data.push.remote.DeviceApi
import com.kiri.data.push.remote.DeviceRegisterRequest

internal class PushDeviceRepositoryImpl @Inject constructor(
    private val api: DeviceApi,
    private val deviceIds: DeviceIdStore,
    private val json: Json,
) : PushDeviceRepository {

    override suspend fun register(fcmToken: String): Result<Unit> = apiCommand(json) {
        api.register(DeviceRegisterRequest(fcmToken = fcmToken, deviceType = DEVICE_TYPE, deviceIdentifier = deviceIds.get()))
    }

    override suspend fun unregister(): Result<Unit> = apiCommand(json) { api.delete(deviceIds.get()) }

    private companion object {
        const val DEVICE_TYPE = "ANDROID"
    }
}
