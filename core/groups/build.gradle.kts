plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:utils"))
            implementation(project(":core:profile"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.qrose.encoder.matrix)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(project(":core:groups:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Why: RoomGroupPinLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
