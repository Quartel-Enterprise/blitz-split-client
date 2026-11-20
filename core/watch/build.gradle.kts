plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:utils"))
            implementation(project(":core:account"))
            // Why: CompleteSignIn and SignOut, so a watch signs in and out as every other device does.
            implementation(project(":core:sync"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:account:testing"))
        }
    }
}
