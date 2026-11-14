plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.presentation)
            api(projects.core.model)
            implementation(projects.core.navigation)
            api(projects.core.account)
            implementation(projects.core.sync)
            // Why: OpenLegalPage, the way Profile's footer opens the legal texts too (the site's page on the web).
            implementation(projects.core.profile)
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.account.testing)
            implementation(projects.core.profile.testing)
        }
    }
}
