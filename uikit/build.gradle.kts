plugins { id("kiri.library"); id("kiri.compose") }
android { namespace = "com.kiri.uikit" }

dependencies {
    // KiriAsyncImage 가 쓴다. 네트워크 모듈은 넣기만 하면 Coil 이 https 주소를 OkHttp 로 받는다
    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
