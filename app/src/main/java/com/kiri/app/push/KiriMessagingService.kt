package com.kiri.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.kiri.data.device.PushDeviceSync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// FCM 메시지를 받는 서비스. 지금은 토큰이 바뀔 때 서버 등록만 바꾼다.
// 앱이 백그라운드일 때 오는 알림 메시지는 FCM 이 직접 띄운다.
// TODO: 서버 알림 형식이 정해지면 onMessageReceived 에서 포그라운드 알림을 띄운다
@AndroidEntryPoint
class KiriMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushDeviceSync: PushDeviceSync

    @Inject
    @PushScope
    lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch { pushDeviceSync.onNewToken(token) }
    }
}
