import java.util.Properties

plugins {
    id("kiri.library")
    id("kiri.hilt")
    id("kiri.serialization")
}

// 서버 주소는 git 에 올리지 않는다. local.properties 에 적고, 없으면 Gradle 속성이나 환경변수(CI)에서 읽는다
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun secret(name: String): String =
    localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: ""

android {
    namespace = "com.kiri.data"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"${secret("KIRI_API_BASE_URL")}\"")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    // 서버 통신
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
dependencies { implementation(libs.kotlinx.coroutines.core) }
