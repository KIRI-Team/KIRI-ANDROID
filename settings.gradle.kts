pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "KIRI ANDROID"
include(":app", ":core", ":uikit", ":domain", ":data")
listOf("home", "auth", "onboarding", "detail", "record", "place", "mypage").forEach {
    include(":feature:$it")
}
