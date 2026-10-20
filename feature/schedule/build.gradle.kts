plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.schedule"; defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }
dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    implementation(project(":feature:detail"))
    implementation(project(":feature:record"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":data"))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
