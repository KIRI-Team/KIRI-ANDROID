plugins {
    id("kiri.library")
    id("kiri.compose")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":uikit"))
}
