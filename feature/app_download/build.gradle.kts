plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            api(project(":core:navigation"))
            implementation(project(":core:account"))
            implementation(project(":core:groups"))
            implementation(project(":core:profile"))
            implementation(project(":core:provider:analytics"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:account:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:profile:testing"))
        }
    }
}
