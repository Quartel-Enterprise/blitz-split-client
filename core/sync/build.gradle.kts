plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.utils)
            implementation(projects.core.account)
            implementation(projects.core.profile)
            implementation(projects.core.groups)
            implementation(libs.quare.connectivity)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
            implementation(projects.core.groups.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
