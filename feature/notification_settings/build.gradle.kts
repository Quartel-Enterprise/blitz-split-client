plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            implementation(project(":core:activity"))
            implementation(project(":core:navigation"))
            implementation(project(":core:profile"))
            implementation(project(":core:sync"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:profile:testing"))
            implementation(project(":core:activity:testing"))
            implementation(project(":core:account:testing"))
        }
    }
}
