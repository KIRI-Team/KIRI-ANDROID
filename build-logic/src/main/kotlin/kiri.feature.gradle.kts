import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("kiri.library")
    id("kiri.compose")
    id("kiri.hilt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(project(":domain"))
    implementation(project(":uikit"))
    implementation(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
}
