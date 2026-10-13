plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.record" }

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.navigation.compose)
    implementation(project(":feature:detail"))
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
android { defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }
