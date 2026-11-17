pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
        maven("https://repo.extendedclip.com/releases/") {
            content { includeGroup("me.clip") }
        }
    }
}

rootProject.name = "nexus-core-parent"

include("nexus-api", "nexus-core")
