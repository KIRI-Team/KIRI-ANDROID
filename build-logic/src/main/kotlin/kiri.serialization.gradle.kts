import org.gradle.api.artifacts.VersionCatalogsExtension

// kotlinx.serialization 을 쓰는 모듈에 적용한다 (@Serializable 코드 생성 + Json 라이브러리)
plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
}
