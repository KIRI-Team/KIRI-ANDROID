plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.home" }
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.kotlinx.coroutines.test)
}
android { defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }
dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
