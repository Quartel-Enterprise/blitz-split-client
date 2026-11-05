plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            // Why: an event is unread unless the device's own account made it.
            implementation(projects.core.account)
            // Why: the In app toggles of Notifications decide which events can be unread.
            implementation(projects.core.profile)
            implementation(projects.core.utils)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.activity.testing)
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Why: RoomActivityLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
