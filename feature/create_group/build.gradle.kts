plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.presentation)
            api(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.utils)
            implementation(projects.core.profile)
            implementation(projects.core.groups)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.profile.testing)
            implementation(projects.core.groups.testing)
        }
    }
}
