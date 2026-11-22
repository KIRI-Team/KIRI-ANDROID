import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kiri.compose")
    id("kiri.hilt")
    alias(libs.plugins.google.services)
}
// 카카오 네이티브 앱 키는 git 에 올리지 않는다 (feature/auth 와 같은 곳에서 읽는다)
val kakaoNativeAppKey: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("KAKAO_NATIVE_APP_KEY")
    ?: providers.gradleProperty("KAKAO_NATIVE_APP_KEY").orNull
    ?: providers.environmentVariable("KAKAO_NATIVE_APP_KEY").orNull
    ?: ""

android {
    namespace = "com.kiri.app"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()
    defaultConfig {
        applicationId = "com.kiri.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // hiltViewModel()을 쓰는 화면을 UI 테스트에서 띄울 수 있도록 Hilt 테스트용 Application으로 실행한다
        testInstrumentationRunner = "com.kiri.app.HiltTestRunner"
        // 카카오톡 일정 초대장 버튼이 앱을 여는 주소(kakao{앱 키}://kakaolink)에 쓴다
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }
    buildTypes {
        release {
            // R8 로 코드를 줄이고 난독화·최적화하며, 쓰지 않는 리소스를 지운다.
            // 라이브러리가 챙기지 않는 규칙은 proguard-rules.pro 에 둔다. 크래시 해석용 mapping.txt 는 빌드마다 보관한다
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation(project(":feature:place"))
    implementation(project(":feature:mypage"))
    implementation(project(":data"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // 푸시 알림(FCM) 토큰을 받아 서버에 기기를 등록한다
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
