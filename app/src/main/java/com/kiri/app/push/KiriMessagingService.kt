package com.kiri.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kiri.data.push.PushDeviceSync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// FCM 메시지를 받는 서비스.
// - 토큰이 바뀌면 서버의 기기 등록을 새 토큰으로 바꾼다
// - 앱이 백그라운드일 때 오는 알림 메시지는 FCM 이 직접 띄우고 이 서비스는 불리지 않는다.
//   앱을 보고 있을 때 온 메시지와 data 메시지만 여기로 와서 직접 띄운다
// TODO: 서버 알림 형식이 정해지면 알림 종류별 채널과 누르면 열 화면(일정 상세 등)을 붙인다
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

    override fun onMessageReceived(message: RemoteMessage) {
        val content = pushContentOf(message.notification?.title, message.notification?.body, message.data) ?: return
        val id = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        showPushNotification(this, content, id)
    }
}
