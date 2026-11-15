plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:profile"))
            api(libs.kotlinx.coroutines.core)
        }
    }
}
