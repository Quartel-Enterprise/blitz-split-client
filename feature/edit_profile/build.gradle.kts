plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.presentation)
            api(projects.core.model)
            implementation(projects.core.navigation)
            api(projects.core.profile)
            implementation(projects.core.account)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
            implementation(projects.core.provider.image)
        }
    }
}
