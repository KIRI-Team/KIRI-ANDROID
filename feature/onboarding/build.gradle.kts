plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.onboarding" }
dependencies {
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.kotlinx.coroutines.test)
}
