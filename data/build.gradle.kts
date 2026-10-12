plugins {
    id("kiri.library")
    id("kiri.hilt")
}
android { namespace = "com.kiri.data" }
dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    // 로그인 토큰 저장
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
}
dependencies { implementation(libs.kotlinx.coroutines.core) }