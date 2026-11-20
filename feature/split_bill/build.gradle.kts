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
            api(project(":core:expenses"))
            implementation(project(":core:groups"))
            implementation(project(":core:settings"))
            implementation(project(":core:support"))
            implementation(project(":core:utils"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
        }
    }
}
