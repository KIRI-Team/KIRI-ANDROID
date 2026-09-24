plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kiri.compose")
}
android {
    namespace = "com.kiri.app"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()
    defaultConfig {
        applicationId = "com.kiri.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":uikit"))
    implementation(project(":feature:home"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:record"))
    implementation(project(":feature:place"))
    implementation(project(":feature:mypage"))
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
