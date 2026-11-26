plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.record" }

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":data"))
    androidTestImplementation(project(":data"))
    implementation(libs.androidx.navigation.compose)
    implementation(project(":feature:detail"))
    implementation(libs.androidx.activity.compose)
    // 일정 초대장을 카카오톡으로 보낸다 (SDK 초기화는 앱 시작 시 SocialLoginInitializer 가 한다)
    implementation(libs.kakao.share)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
android { defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }

android { testOptions { targetSdk = libs.versions.targetSdk.get().toInt() } }
