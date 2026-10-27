rootProject.name = "BlitzSplit"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Shared Kotlin: the umbrella every platform consumes (iOS/macOS framework, JS library)
include(":shared")

// Core: domain and data shared by the features
include(":core:model")
include(":core:utils")
include(":core:presentation")
include(":core:navigation")
include(":core:profile")
include(":core:profile:testing")
include(":core:account")
include(":core:account:testing")
include(":core:sync")
include(":core:provider:room")
include(":core:provider:supabase")
include(":core:provider:image")

// Features: presentation (ViewModel, UiState, UiEvent, UiAction) shared by every UI
include(":feature:welcome")
include(":feature:profile_setup")
include(":feature:restore")
include(":feature:recovery_code")
include(":feature:groups")

// Android: the native Jetpack Compose UI of each feature
include(":androidApp")
include(":android:designsystem")
include(":android:platform")
include(":android:feature:welcome")
include(":android:feature:profile_setup")
include(":android:feature:restore")
include(":android:feature:recovery_code")
include(":android:feature:groups")
