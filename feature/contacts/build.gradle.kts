plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            // Why: the rows and the profile draw core:contacts' Contact, ContactNet and ContactHistoryItem.
            api(project(":core:contacts"))
            api(project(":core:navigation"))
            implementation(project(":core:profile"))
            implementation(project(":core:account"))
            implementation(project(":core:groups"))
            implementation(project(":core:expenses"))
            implementation(project(":core:utils"))
            implementation(project(":core:activity"))
            implementation(project(":core:sync"))
            implementation(project(":core:settings"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:contacts:testing"))
            implementation(project(":core:activity:testing"))
        }
    }
}
