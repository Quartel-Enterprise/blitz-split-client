plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

// The modules SwiftUI and React talk to directly: their ViewModels, UiState, UiEvent and UiAction
// types are exported to the Apple framework (Swift sees them by name). Listed by path and turned into
// dependencies below: at the top of a build script, project(":core:model") is the Project itself.
private val exportedToApple = listOf(
    ":core:model",
    ":core:presentation",
    ":core:navigation",
    ":core:expenses",
    ":core:activity",
    ":core:contacts",
    ":core:legal",
    ":core:release_notes",
    ":core:support",
    ":core:provider:analytics",
    ":feature:welcome",
    ":feature:profile_setup",
    ":feature:restore",
    ":feature:recovery_code",
    ":feature:groups",
    ":feature:profile",
    ":feature:edit_profile",
    ":feature:payment_key",
    ":feature:paying_method",
    ":feature:delete_account",
    ":feature:contact_support",
    ":feature:notification_settings",
    ":feature:create_group",
    ":feature:invite",
    ":feature:members",
    ":feature:join",
    ":feature:group_detail",
    ":feature:bill_detail",
    ":feature:split_bill",
    ":feature:edit_group",
    ":feature:register_payment",
    ":feature:global_balances",
    ":feature:group_stats",
    ":feature:category_map",
    ":feature:activity",
    ":feature:contacts",
    ":feature:in_app_update",
    ":feature:legal",
    ":feature:release_notes",
    ":feature:app_download",
).map { dependencies.project(it) }

kotlin {
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            exportedToApple.forEach { export(it) }
            export(libs.quare.loadable)
        }
    }

    js {
        outputModuleName = "blitzsplit-shared"
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            exportedToApple.forEach { api(it) }
            api(libs.quare.loadable)
            implementation(project(":core:utils"))
            implementation(project(":core:account"))
            implementation(project(":core:profile"))
            implementation(project(":core:groups"))
            implementation(project(":core:contacts"))
            implementation(project(":core:sync"))
            implementation(project(":core:settings"))
            implementation(project(":core:in_app_update"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(project(":core:provider:image"))
            implementation(project(":core:provider:crashlytics"))
            implementation(libs.quare.connectivity)
            implementation(libs.kermit)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        // The store screenshots' seed (docs/store-listing-screenshots.md) writes the sample trip's feed and links.
        appleMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.sqlite.bundled)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.functions)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
        }
    }
}
