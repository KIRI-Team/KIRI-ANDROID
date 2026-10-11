plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.mypage" }

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.kotlinx.coroutines.test)
}
