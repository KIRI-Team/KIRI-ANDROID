buildscript {
    dependencies {
        // 루트 클래스패스의 AGP가 끌어오는 구버전 javapoet(1.10) 때문에 hiltAggregateDeps가 실패하는 것을 막는다.
        classpath(libs.javapoet)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
