pluginManagement {
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
    }
}

plugins {
    // Auto-provision JDK 21 if it's not installed locally.
    // See gradle/libs.versions.toml > [versions].* — modules pin jvmToolchain(21).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "newton-field-app"

include(":app")

// Core modules
include(":core:common")
include(":core:ui")
include(":core:logging")
include(":core:bluetooth")

// GNSS modules
include(":gnss:data")
include(":gnss:command")
include(":gnss:ntrip")

// Domain-adjacent
include(":crs")

// Business layer
include(":domain")
include(":data")

// Feature modules
include(":features:project")
include(":features:settings")
include(":features:survey")
include(":features:cad")
