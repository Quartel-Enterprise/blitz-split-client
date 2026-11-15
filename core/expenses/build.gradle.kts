plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.kotlinx.datetime)
            // Why: a receipt read (ReceiptScan) hands its encoded photo to AttachBillImage as the feature got it.
            api(project(":core:provider:image"))
            // Why: Saldos and the exit dialogs name a debt's members as the group has them (debtRowOf, faceOf).
            implementation(project(":core:groups"))
            implementation(project(":core:utils"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:groups:testing"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            // Why: RoomExpenseLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
