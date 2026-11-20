rootProject.name = "BlitzSplit"

// Modules name each other by path, project(":core:model"). TYPESAFE_PROJECT_ACCESSORS stays off, and the root
// build.gradle.kts fails the build if it comes back: docs/ci.md, "Speed and timeouts", has what it cost.

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
include(":core:activity")
include(":core:activity:testing")
include(":core:contacts")
include(":core:contacts:testing")
include(":core:sync")
include(":core:watch")
include(":core:settings")
include(":core:in_app_update")
include(":core:legal")
include(":core:release_notes")
include(":core:support")
include(":core:provider:room")
include(":core:provider:supabase")
include(":core:provider:image")
include(":core:provider:crashlytics")
include(":core:provider:analytics")

// Features: presentation (ViewModel, UiState, UiEvent, UiAction) shared by every UI
include(":feature:welcome")
include(":feature:watch_pairing")
include(":feature:watch_balances")
include(":feature:connect_watch")
include(":feature:profile_setup")
include(":feature:restore")
include(":feature:recovery_code")
include(":feature:groups")
include(":feature:profile")
include(":feature:edit_profile")
include(":feature:payment_key")
include(":feature:paying_method")
include(":feature:delete_account")
include(":feature:contact_support")
include(":feature:notification_settings")
include(":feature:create_group")
include(":feature:invite")
include(":feature:members")
include(":feature:join")
include(":feature:split_bill")
include(":feature:group_detail")
include(":feature:bill_detail")
include(":feature:edit_group")
include(":feature:register_payment")
include(":feature:global_balances")
include(":feature:group_stats")
include(":feature:category_map")
include(":feature:activity")
include(":feature:contacts")
include(":feature:in_app_update")
include(":feature:legal")
include(":feature:release_notes")
include(":feature:app_download")

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
include(":android:feature:paying_method")
include(":android:feature:delete_account")
include(":android:feature:connect_watch")
include(":android:feature:contact_support")
include(":android:feature:notification_settings")
include(":android:feature:legal")
include(":android:feature:release_notes")
include(":android:feature:create_group")
include(":android:feature:invite")
include(":android:feature:members")
include(":android:feature:join")
include(":android:feature:group_detail")
include(":android:feature:bill_detail")
include(":android:feature:split_bill")
include(":android:feature:edit_group")
include(":android:feature:register_payment")
include(":android:feature:global_balances")
include(":android:feature:contacts")
include(":android:feature:group_stats")
include(":android:feature:category_map")
include(":android:feature:activity")
include(":android:feature:in_app_update")
include(":android:store_screenshots")
