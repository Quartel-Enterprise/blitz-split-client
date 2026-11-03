plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.kotlinx.coroutines.core)
            // Why: a UiState section that loads on its own is a Loadable, so "not yet" never reads as empty.
            api(libs.quare.loadable)
        }
    }
}
