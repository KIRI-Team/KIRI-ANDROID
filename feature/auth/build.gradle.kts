plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.auth" }
dependencies {
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.kotlinx.coroutines.test)
}
