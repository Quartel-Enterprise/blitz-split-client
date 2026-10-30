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
include(":core:groups")
include(":core:groups:testing")
include(":core:expenses")
include(":core:expenses:testing")
include(":core:sync")
include(":core:settings")
include(":core:provider:room")
include(":core:provider:supabase")
include(":core:provider:image")

// Features: presentation (ViewModel, UiState, UiEvent, UiAction) shared by every UI
include(":feature:welcome")
include(":feature:profile_setup")
include(":feature:restore")
include(":feature:recovery_code")
include(":feature:groups")
include(":feature:profile")
include(":feature:edit_profile")
include(":feature:payment_key")
include(":feature:delete_account")
include(":feature:notification_settings")
include(":feature:create_group")
include(":feature:invite")
include(":feature:members")
include(":feature:join")
include(":feature:split_bill")
include(":feature:group_detail")
include(":feature:bill_detail")

// Android: the native Jetpack Compose UI of each feature
include(":androidApp")
include(":android:designsystem")
include(":android:platform")
include(":android:feature:welcome")
include(":android:feature:profile_setup")
include(":android:feature:restore")
include(":android:feature:recovery_code")
include(":android:feature:groups")
include(":android:feature:profile")
include(":android:feature:edit_profile")
include(":android:feature:payment_key")
include(":android:feature:delete_account")
include(":android:feature:notification_settings")
include(":android:feature:create_group")
include(":android:feature:invite")
include(":android:feature:members")
include(":android:feature:join")
include(":android:feature:group_detail")
include(":android:feature:bill_detail")
include(":android:feature:split_bill")
