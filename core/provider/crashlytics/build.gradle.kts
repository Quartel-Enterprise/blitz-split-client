plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.crashkios.crashlytics)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        // The SDK CrashKiOS calls on Android
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.crashlytics)
        }
    }
}
