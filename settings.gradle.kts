pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://devrepo.kakao.com/nexus/content/groups/public/")
    }
}
rootProject.name = "KIRI ANDROID"
include(":app", ":core", ":uikit", ":domain", ":data")
listOf("home", "auth", "onboarding", "detail", "record", "place", "mypage", "schedule").forEach {
    include(":feature:$it")
}
