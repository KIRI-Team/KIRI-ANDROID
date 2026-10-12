package com.kiri.feature.auth.login

import android.app.Activity
import com.kiri.feature.auth.model.LoginProvider
import javax.inject.Inject

// 실제 카카오·구글 SDK로 로그인한다
class SdkSocialLogin @Inject constructor(
    private val kakao: KakaoLogin,
    private val google: GoogleLogin,
) : SocialLogin {
    override suspend fun login(activity: Activity, provider: LoginProvider): SocialLoginResult = when (provider) {
        LoginProvider.KAKAO -> kakao.login(activity)
        LoginProvider.GOOGLE -> google.login(activity)
    }
}
