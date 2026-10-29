plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

// The modules SwiftUI and React talk to directly: their ViewModels, UiState, UiEvent and UiAction
// types are exported to the Apple framework (Swift sees them by name).
private val exportedToApple = listOf(
    projects.core.model,
    projects.core.presentation,
    projects.core.navigation,
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
)

kotlin {
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            exportedToApple.forEach { export(it) }
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
            implementation(projects.core.utils)
            implementation(projects.core.account)
            implementation(projects.core.profile)
            implementation(projects.core.groups)
            implementation(projects.core.sync)
            implementation(projects.core.settings)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(projects.core.provider.image)
            implementation(libs.quare.connectivity)
            implementation(libs.kermit)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
        }
    }
}
