import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
android {
    compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
    buildToolsVersion = libs.findVersion("buildTools").get().requiredVersion
    defaultConfig { minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }

dependencies { add("testImplementation", libs.findLibrary("junit").get()) }
