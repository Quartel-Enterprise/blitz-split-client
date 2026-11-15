plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            // Why: the items draw core:activity's kinds, types and fields, and the days core:expenses' RelativeDay.
            api(project(":core:activity"))
            api(project(":core:expenses"))
            api(project(":core:navigation"))
            implementation(project(":core:profile"))
            implementation(project(":core:account"))
            implementation(project(":core:sync"))
            implementation(project(":core:groups"))
            implementation(project(":core:utils"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:activity:testing"))
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
        }
    }
}
