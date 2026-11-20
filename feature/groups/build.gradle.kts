plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            // Why: the dialogs of "Sair do grupo" and "Apagar grupo" draw core:expenses' models, as the group's do.
            api(project(":core:expenses"))
            // Why: AllExpensesViewModel takes its Route.AllExpenses, which the UIs hand it.
            api(project(":core:navigation"))
            implementation(project(":core:profile"))
            implementation(project(":core:account"))
            implementation(project(":core:sync"))
            implementation(project(":core:groups"))
            implementation(project(":core:settings"))
            implementation(project(":core:utils"))
            implementation(project(":core:activity"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:account:testing"))
            implementation(project(":core:profile:testing"))
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:activity:testing"))
        }
    }
}
