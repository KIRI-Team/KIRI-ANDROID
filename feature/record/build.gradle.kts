plugins { id("kiri.feature") }
android { namespace = "com.kiri.feature.record" }

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(project(":feature:detail"))
}
