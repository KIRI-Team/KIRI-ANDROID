package com.kiri.uikit.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// 알림 권한 창을 띄워도 되는지. 앱 화면(MainActivity)에서만 켠다.
// 화면 하나만 띄우는 UI 테스트에서 시스템 권한 창이 화면을 가리지 않도록 기본값은 꺼 둔다
val LocalNotificationPermissionPromptEnabled = staticCompositionLocalOf { false }

// 알림 권한을 묻는 때. 처음은 약관 동의 화면에 들어올 때, 거절했으면 홈 화면에서 한 번 더 묻는다
enum class NotificationPermissionMoment { TERMS, HOME }

// 이 기기에서 이미 물어본 때
internal data class NotificationPermissionRecord(
    val askedOnTerms: Boolean = false,
    val askedOnHome: Boolean = false,
)

// Android 13(API 33) 미만은 권한 없이 알림이 보이므로 묻지 않는다. 이미 허용했으면 묻지 않는다.
// 약관 화면: 아직 한 번도 묻지 않았을 때만 (로그인을 다시 해도 또 묻지 않는다).
// 홈 화면: 홈에서 아직 묻지 않았을 때 한 번. 약관 화면에서 방금 거절한 사람에게 바로 다시 묻지 않도록
// 같은 실행(앱을 켠 뒤 끌 때까지) 안에서 이미 물었으면 다음 실행으로 미룬다.
// 안드로이드는 두 번 거절하면 더 이상 창을 띄우지 않으므로 앱도 두 번까지만 묻는다
internal fun shouldAskNotificationPermission(
    sdkInt: Int,
    granted: Boolean,
    moment: NotificationPermissionMoment,
    record: NotificationPermissionRecord,
    askedThisLaunch: Boolean,
): Boolean {
    if (sdkInt < Build.VERSION_CODES.TIRAMISU || granted) return false
    return when (moment) {
        NotificationPermissionMoment.TERMS -> !record.askedOnTerms && !record.askedOnHome
        NotificationPermissionMoment.HOME -> !record.askedOnHome && !askedThisLaunch
    }
}

// 화면에 들어왔을 때 조건이 맞으면 알림 권한 창을 띄운다. 허용·거절 결과는 따로 쓰지 않는다
// (알림 설정 화면은 들어올 때마다 실제 권한을 다시 읽는다)
@Composable
fun NotificationPermissionPrompt(moment: NotificationPermissionMoment) {
    if (!LocalNotificationPermissionPromptEnabled.current || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(moment) {
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val record = NotificationPermissionRecord(
            askedOnTerms = prefs.getBoolean(ASKED_ON_TERMS, false),
            askedOnHome = prefs.getBoolean(ASKED_ON_HOME, false),
        )
        if (!shouldAskNotificationPermission(Build.VERSION.SDK_INT, granted, moment, record, LaunchState.asked)) return@LaunchedEffect
        val key = if (moment == NotificationPermissionMoment.TERMS) ASKED_ON_TERMS else ASKED_ON_HOME
        prefs.edit().putBoolean(key, true).apply()
        LaunchState.asked = true
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

// 앱을 켠 뒤 권한을 물었는지. 앱이 종료되면 초기화된다
private object LaunchState {
    @Volatile
    var asked = false
}

private const val PREFS = "notification_permission"
private const val ASKED_ON_TERMS = "asked_on_terms"
private const val ASKED_ON_HOME = "asked_on_home"
