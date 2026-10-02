plugins {
    id("kiri.library")
    id("kiri.hilt")
}
android { namespace = "com.kiri.data" }
dependencies { implementation(project(":domain")); implementation(project(":core")) }
dependencies { implementation(libs.kotlinx.coroutines.core) }
