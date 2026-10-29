plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(projects.core.utils)
            implementation(projects.core.profile)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.qrose.encoder.matrix)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.groups.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
