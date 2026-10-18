plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.account)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
