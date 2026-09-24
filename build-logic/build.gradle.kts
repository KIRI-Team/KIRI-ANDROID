plugins { `kotlin-dsl` }

kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
}
