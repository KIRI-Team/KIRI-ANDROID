package com.kiri.app

import android.app.Application
import com.kiri.app.push.PushScope
import com.kiri.app.push.createNotificationChannels
import com.kiri.data.push.PushDeviceSync
import com.kiri.feature.auth.login.SocialLoginInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class KiriApplication : Application() {

    @Inject
    lateinit var pushDeviceSync: PushDeviceSync

    @Inject
    @PushScope
    lateinit var pushScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        SocialLoginInitializer.init(this)
        createNotificationChannels(this)
        // 로그인한 동안 이 기기를 푸시 알림 대상으로 서버에 등록해 둔다
        pushScope.launch { pushDeviceSync.run() }
    }
}
