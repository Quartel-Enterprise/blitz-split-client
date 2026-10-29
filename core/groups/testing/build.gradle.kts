plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.groups)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
