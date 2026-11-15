plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            implementation(project(":core:navigation"))
            api(project(":core:account"))
            implementation(project(":core:sync"))
            // Why: OpenLegalPage, the way Profile's footer opens the legal texts too (the site's page on the web).
            implementation(project(":core:profile"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
        }
    }
}
