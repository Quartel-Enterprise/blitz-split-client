plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.profile)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
