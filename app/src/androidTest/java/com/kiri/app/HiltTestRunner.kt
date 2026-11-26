package com.kiri.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// UI 테스트를 KiriApplication 대신 HiltTestApplication으로 실행한다
// (@HiltAndroidTest 테스트에서 Hilt가 적용된 Activity와 hiltViewModel()을 쓸 수 있게 된다)
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
