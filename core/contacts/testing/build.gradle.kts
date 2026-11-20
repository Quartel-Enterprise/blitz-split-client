plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:contacts"))
            api(libs.kotlinx.coroutines.core)
        }
    }
}
