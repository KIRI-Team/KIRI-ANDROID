plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kiri.compose")
    id("kiri.hilt")
}
android {
    namespace = "com.kiri.app"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()
    defaultConfig {
        applicationId = "com.kiri.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // hiltViewModel()을 쓰는 화면을 UI 테스트에서 띄울 수 있도록 Hilt 테스트용 Application으로 실행한다
        testInstrumentationRunner = "com.kiri.app.HiltTestRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":uikit"))
    implementation(project(":domain"))
    // Hilt가 :data의 Repository 바인딩을 찾을 수 있도록 앱에 포함한다
    implementation(project(":data"))
    implementation(project(":feature:home"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:record"))
    implementation(project(":feature:schedule"))
    implementation(project(":feature:place"))
    implementation(project(":feature:mypage"))
    implementation(project(":data"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
