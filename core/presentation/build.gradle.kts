plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.kotlinx.coroutines.core)
            // Why: a UiState section that loads on its own is a Loadable, so "not yet" never reads as empty.
            api(libs.quare.loadable)
            // Why: TrackedViewModel's TrackEvent, which every feature's ViewModel hands it.
            api(project(":core:provider:analytics"))
            // Why: the photo editor's events, PhotoCropEvent, which PhotoCropAnalytics names.
            api(project(":core:model"))
        }
    }
}
