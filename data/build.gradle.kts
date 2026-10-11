plugins {
    id("kiri.library")
    id("kiri.hilt")
}
android { namespace = "com.kiri.data" }
dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    // 카카오·구글 로그인
    implementation(libs.kakao.user)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // 로그인 토큰 저장
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
}
dependencies { implementation(libs.kotlinx.coroutines.core) }