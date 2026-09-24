import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins { id("org.jetbrains.kotlin.plugin.compose") }

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
}
pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> { buildFeatures.compose = true }
}
dependencies {
    val bom = platform(libs.findLibrary("androidx-compose-bom").get())
    add("implementation", bom)
    add("testImplementation", bom)
    add("androidTestImplementation", bom)
    add("implementation", libs.findLibrary("androidx-compose-ui").get())
    add("implementation", libs.findLibrary("androidx-compose-material3").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
}
