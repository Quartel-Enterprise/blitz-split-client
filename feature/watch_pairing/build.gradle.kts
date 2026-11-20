plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            api(project(":core:navigation"))
            implementation(project(":core:utils"))
            implementation(project(":core:watch"))
            implementation(project(":core:profile"))
            implementation(project(":core:sync"))
            implementation(project(":core:provider:analytics"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:profile:testing"))
        }
    }
}
