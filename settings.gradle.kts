pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id in setOf("com.apexfission.android-repo-setup.root", "com.apexfission.android-repo-setup")) {
                requested.version?.let { useModule("com.github.lambdawalker:gradle.android-repo-setup:$it") }
            }
        }
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") { content { includeModule("com.github.lambdawalker", "gradle.android-repo-setup") } }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io"); content { includeGroup("com.github.lambdawalker") } }
    }
}

rootProject.name = "android.apexfission.auth"
include(":app")
include(":auth")
