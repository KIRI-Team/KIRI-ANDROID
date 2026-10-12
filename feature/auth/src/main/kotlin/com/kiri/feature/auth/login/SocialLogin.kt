package com.kiri.feature.auth.login

import android.app.Activity
import com.kiri.feature.auth.model.LoginProvider

// 소셜 로그인 실행. SDK 로그인 창은 Activity 위에 떠야 해서 화면(Route)에서 Activity를 넘겨 호출한다
// UI 테스트에서는 실제 SDK 대신 가짜 구현으로 바꿔 끼운다
interface SocialLogin {
    suspend fun login(activity: Activity, provider: LoginProvider): SocialLoginResult
}
