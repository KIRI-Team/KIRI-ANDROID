import java.util.Properties

plugins { id("kiri.feature") }

// 소셜 로그인 키는 git 에 올리지 않는다. local.properties 에 적고, 없으면 Gradle 속성이나 환경변수(CI)에서 읽는다
// 키가 없어도 빌드는 되지만 로그인은 동작하지 않는다
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun secret(name: String): String =
    localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: ""

val kakaoNativeAppKey = secret("KAKAO_NATIVE_APP_KEY")
val googleWebClientId = secret("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.kiri.feature.auth"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        // 카카오 로그인 뒤 앱으로 돌아오는 주소(kakao{앱 키}://oauth)에 쓴다
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)

    // 카카오·구글 로그인 (SDK 로그인은 Activity가 필요해서 화면 모듈에서 실행한다)
    implementation(libs.kakao.user)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.kotlinx.coroutines.test)
}
