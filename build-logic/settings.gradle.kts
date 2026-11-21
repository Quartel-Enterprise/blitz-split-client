rootProject.name = "build-logic"

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
