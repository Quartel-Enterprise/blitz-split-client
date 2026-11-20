plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
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
            implementation(project(":core:sync"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.quare.connectivity)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:groups:testing"))
        }
    }
}
