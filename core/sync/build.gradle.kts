plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:utils"))
            implementation(project(":core:navigation"))
            implementation(project(":core:account"))
            implementation(project(":core:profile"))
            implementation(project(":core:groups"))
            implementation(project(":core:settings"))
            implementation(project(":core:expenses"))
            implementation(project(":core:activity"))
            implementation(project(":core:contacts"))
            implementation(project(":core:provider:crashlytics"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.quare.connectivity)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:activity:testing"))
            implementation(project(":core:contacts:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
