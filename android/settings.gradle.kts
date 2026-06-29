pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// Lets Gradle auto-provision the JDK 11 toolchain (jvmToolchain(11)) on machines/CI that
// only have a newer JDK installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // PebbleKit Android 2 is published here; jitpack also covers the GMapsParser
        // reference library if you ever choose to depend on it directly.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PebbleMapsNav"
include(":app")
