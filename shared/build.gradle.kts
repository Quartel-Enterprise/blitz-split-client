plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

// The modules SwiftUI and React talk to directly: their ViewModels, UiState, UiEvent and UiAction
// types are exported to the Apple framework (Swift sees them by name).
private val exportedToApple = listOf(
    projects.core.model,
    projects.core.presentation,
    projects.core.navigation,
    projects.core.expenses,
    projects.core.activity,
    projects.core.contacts,
    projects.core.provider.analytics,
    projects.feature.welcome,
    projects.feature.profileSetup,
    projects.feature.restore,
    projects.feature.recoveryCode,
    projects.feature.groups,
    projects.feature.profile,
    projects.feature.editProfile,
    projects.feature.paymentKey,
    projects.feature.deleteAccount,
    projects.feature.notificationSettings,
    projects.feature.createGroup,
    projects.feature.invite,
    projects.feature.members,
    projects.feature.join,
    projects.feature.groupDetail,
    projects.feature.billDetail,
    projects.feature.splitBill,
    projects.feature.editGroup,
    projects.feature.registerPayment,
    projects.feature.globalBalances,
    projects.feature.groupStats,
    projects.feature.categoryMap,
    projects.feature.activity,
    projects.feature.contacts,
    projects.feature.inAppUpdate,
    projects.feature.appDownload,
)

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
            implementation(projects.core.utils)
            implementation(projects.core.account)
            implementation(projects.core.profile)
            implementation(projects.core.groups)
            implementation(projects.core.contacts)
            implementation(projects.core.sync)
            implementation(projects.core.settings)
            implementation(projects.core.inAppUpdate)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(projects.core.provider.image)
            implementation(projects.core.provider.crashlytics)
            implementation(libs.quare.connectivity)
            implementation(libs.kermit)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        // The store screenshots' seed (docs/store-listing-screenshots.md) writes the sample trip's feed and links.
        appleMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
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
