plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.presentation)
            api(projects.core.model)
            // Why: the dialogs of "Sair do grupo" and "Apagar grupo" draw core:expenses' models, as the group's do.
            api(projects.core.expenses)
            implementation(projects.core.navigation)
            implementation(projects.core.profile)
            implementation(projects.core.account)
            implementation(projects.core.sync)
            implementation(projects.core.groups)
            implementation(projects.core.settings)
            implementation(projects.core.utils)
            implementation(projects.core.activity)
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
            implementation(projects.core.groups.testing)
            implementation(projects.core.expenses.testing)
            implementation(projects.core.activity.testing)
        }
    }
}
