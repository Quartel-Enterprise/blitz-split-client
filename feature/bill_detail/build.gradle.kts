plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            api(project(":core:navigation"))
            api(project(":core:expenses"))
            implementation(project(":core:groups"))
            implementation(project(":core:account"))
            implementation(project(":core:activity"))
            implementation(project(":core:profile"))
            implementation(project(":core:settings"))
            implementation(project(":core:sync"))
            implementation(project(":core:utils"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.quare.connectivity)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:account:testing"))
            implementation(project(":core:activity:testing"))
            implementation(project(":core:profile:testing"))
        }
    }
}
