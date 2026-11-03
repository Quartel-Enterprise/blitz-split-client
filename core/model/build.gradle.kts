plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        // Why: the convention plugin opts every module into ExperimentalCoroutinesApi, a marker Kotlin can't find
        // in a module without coroutines on its classpath.
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
