plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            // Why: a contact is someone the groups have, with the balance core:expenses works out.
            api(project(":core:groups"))
            api(project(":core:expenses"))
            implementation(project(":core:utils"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(project(":core:contacts:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
