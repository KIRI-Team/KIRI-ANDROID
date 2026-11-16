package com.kiri.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kiri.app.MainActivity
import com.kiri.app.R

// 서버 알림 종류가 정해지기 전까지 모든 푸시를 받는 채널. AndroidManifest 의
// default_notification_channel_id 와 같은 값이라 앱이 백그라운드일 때 FCM 이 띄우는 알림도 여기로 온다.
// TODO: 서버 알림 종류가 정해지면 일정·질문 등으로 채널을 나눈다 (기기 설정에서 채널별로 끌 수 있다)
internal const val GENERAL_CHANNEL_ID = "kiri_general"

// 채널은 한 번 만들면 이름·설명만 바꿀 수 있고, 같은 id 로 다시 만들어도 사용자가 바꾼 설정은 그대로다
internal fun createNotificationChannels(context: Context) {
    val channel = NotificationChannel(GENERAL_CHANNEL_ID, "KIRI 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "가족 일정, 오늘의 질문 등 KIRI 소식"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

// 알림에 보여줄 제목과 본문
internal data class PushContent(val title: String?, val body: String?)

// FCM 의 notification 영역을 먼저 쓰고, 없으면 data 의 title·body 를 쓴다.
// 서버 메시지 형식이 정해지지 않아 두 방식을 모두 받는다. 둘 다 비어 있으면 보여줄 것이 없어 null
internal fun pushContentOf(notificationTitle: String?, notificationBody: String?, data: Map<String, String>): PushContent? {
    val title = notificationTitle.nonBlank() ?: data["title"].nonBlank()
    val body = notificationBody.nonBlank() ?: data["body"].nonBlank()
    return if (title == null && body == null) null else PushContent(title, body)
}

private fun String?.nonBlank() = this?.takeIf { it.isNotBlank() }

// 앱을 보고 있을 때 온 푸시는 FCM 이 띄워주지 않아 직접 띄운다. 누르면 앱을 연다.
// 알림 권한이 없거나 사용자가 알림을 꺼 두었으면 띄우지 않는다
internal fun showPushNotification(context: Context, content: PushContent, notificationId: Int) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(context.getColor(R.color.kiri_notification_color))
        .setContentTitle(content.title ?: context.getString(R.string.app_name))
        .setContentText(content.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
        .setAutoCancel(true)
        .setContentIntent(openApp)
        .build()
    try {
        manager.notify(notificationId, notification)
    } catch (e: SecurityException) {
        // 확인한 뒤 바로 권한이 꺼진 경우. 보여주지 못해도 앱이 멈추지 않게 한다
    }
}
