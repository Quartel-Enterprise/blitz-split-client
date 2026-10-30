plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(libs.kotlinx.datetime)
            implementation(projects.core.utils)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.expenses.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Why: RoomExpenseLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
