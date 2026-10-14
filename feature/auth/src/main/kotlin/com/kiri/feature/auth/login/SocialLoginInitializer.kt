package com.kiri.feature.auth.login

import android.content.Context
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kiri.feature.auth.BuildConfig

// 소셜 로그인 SDK 초기화. 앱 시작 시(KiriApplication.onCreate) 한 번 호출한다
object SocialLoginInitializer {
    private const val TAG = "SocialLogin"

    fun init(context: Context) {
        // 키를 아직 설정하지 않은 환경(새로 받은 저장소, CI 테스트 등)에서는 초기화하지 않고 앱은 그대로 실행한다
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            Log.w(TAG, "KAKAO_NATIVE_APP_KEY 가 없어 카카오 SDK를 초기화하지 않습니다. local.properties 를 확인하세요.")
            return
        }
        KakaoSdk.init(context, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
