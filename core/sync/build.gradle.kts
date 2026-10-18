plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.utils)
            implementation(projects.core.account)
            implementation(projects.core.profile)
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
