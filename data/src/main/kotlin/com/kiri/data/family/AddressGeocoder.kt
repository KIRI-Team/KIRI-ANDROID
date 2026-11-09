package com.kiri.data.family

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.Locale
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal data class AddressCoordinates(val latitude: Double, val longitude: Double)
internal fun interface AddressGeocoder {
    suspend fun resolve(address: String): AddressCoordinates?
}

/** 우편번호 서비스는 좌표를 제공하지 않으므로 선택한 기본 주소만 기기 위치 서비스로 변환한다. */
internal class DeviceAddressGeocoder(private val context: Context) : AddressGeocoder {
    @Suppress("DEPRECATION")
    override suspend fun resolve(address: String): AddressCoordinates? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) throw IOException("이 기기에서 주소 위치 확인을 사용할 수 없어요. 다른 기기에서 다시 시도해주세요.")
        val geocoder = Geocoder(context, Locale.KOREA)
        val addresses = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(address, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resumeWithException(IOException("주소 위치를 확인하지 못했어요. 잠시 후 다시 선택해주세요."))
                    }
                })
            }
        } else geocoder.getFromLocationName(address, 5).orEmpty()
        addresses.firstOrNull { it.hasLatitude() && it.hasLongitude() }?.let { AddressCoordinates(it.latitude, it.longitude) }
    }
}

@Module @InstallIn(SingletonComponent::class)
internal object AddressGeocoderModule {
    @Provides @Singleton fun geocoder(@ApplicationContext context: Context): AddressGeocoder = DeviceAddressGeocoder(context)
}
