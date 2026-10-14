package com.kiri.app

import android.app.Application
import com.kiri.feature.auth.login.SocialLoginInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KiriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SocialLoginInitializer.init(this)
    }
}
