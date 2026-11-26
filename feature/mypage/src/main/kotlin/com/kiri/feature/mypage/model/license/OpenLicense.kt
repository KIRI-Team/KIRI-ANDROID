package com.kiri.feature.mypage.model.license

// 앱에서 쓰는 오픈소스 라이브러리와 라이선스
data class OpenLicense(
    val name: String,
    val version: String,
    val description: String,
    val licenseName: String,
    val notice: String,
) {
    companion object {
        // TODO: gradle/libs.versions.toml 의 버전을 바꾸면 여기 버전도 맞춘다
        val Libraries: List<OpenLicense> = listOf(
            apache("Jetpack Compose", "BOM 2025.12.00", "Android의 선언형 UI 툴킷", "The Android Open Source Project"),
            apache("Compose Material 3", "BOM 2025.12.00", "Material Design 3 컴포넌트", "The Android Open Source Project"),
            apache("AndroidX Activity Compose", "1.12.4", "Activity와 Compose 연결", "The Android Open Source Project"),
            apache("AndroidX Navigation Compose", "2.9.8", "Compose 화면 이동", "The Android Open Source Project"),
            apache("AndroidX Lifecycle", "2.9.2", "ViewModel과 생명주기 관리", "The Android Open Source Project"),
            apache("AndroidX Hilt", "1.3.0", "Hilt ViewModel과 Compose 연결", "The Android Open Source Project"),
            apache("Dagger Hilt", "2.57.2", "의존성 주입", "The Dagger Authors"),
            apache("Kotlin Coroutines", "1.9.0", "비동기 처리", "JetBrains s.r.o. and Kotlin Programming Language contributors"),
            apache("Kotlin Standard Library", "2.2.21", "Kotlin 기본 라이브러리", "JetBrains s.r.o. and Kotlin Programming Language contributors"),
        )

        private fun apache(name: String, version: String, description: String, holder: String) = OpenLicense(
            name = name,
            version = version,
            description = description,
            licenseName = "Apache License 2.0",
            notice = APACHE_2_NOTICE.replace("{holder}", holder),
        )
    }
}

// 팝업 너비에 맞게 줄바꿈되도록 문단 안에서는 줄을 나누지 않는다
private const val APACHE_2_NOTICE = """Copyright {holder}

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."""
