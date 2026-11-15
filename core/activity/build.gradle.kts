plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            // Why: an event is unread unless the device's own account made it.
            implementation(project(":core:account"))
            // Why: the In app toggles of Notifications decide which events can be unread.
            implementation(project(":core:profile"))
            implementation(project(":core:utils"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(project(":core:activity:testing"))
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Why: RoomActivityLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
