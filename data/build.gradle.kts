plugins { id("kiri.library") }
android { namespace = "com.kiri.data" }
dependencies { implementation(project(":domain")); implementation(project(":core")) }
