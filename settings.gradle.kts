pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" -> useVersion("8.1.0")
                "org.jetbrains.kotlin.android" -> useVersion("1.9.10")
                "com.google.devtools.ksp" -> useVersion("1.9.0-1.0.12")
            }
        }
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "DailyQuiz"
include(":app")