plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.presentation)
            api(projects.core.model)
            api(projects.core.navigation)
            // Why: Divisão padrão's exceptions name categories (core:expenses) and keep CategorySplit (core:groups).
            api(projects.core.groups)
            api(projects.core.expenses)
            implementation(projects.core.utils)
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.groups.testing)
            implementation(projects.core.expenses.testing)
        }
    }
}
