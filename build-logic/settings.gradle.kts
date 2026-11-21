rootProject.name = "build-logic"

// The `kotlin-dsl` plugin and the Kotlin libraries it pulls in resolve from these repositories, not from
// dependencyResolutionManagement's. Without the block it is the Plugin Portal alone, which sends what it
// doesn't host itself on to Maven Central, 429s included.
pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "MavenCentralMirror"
            allowInsecureContinueWhenDisabled = true
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        // The mirror before Maven Central, as in the root settings.gradle.kts.
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "MavenCentralMirror"
            allowInsecureContinueWhenDisabled = true
        }
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(":convention")
