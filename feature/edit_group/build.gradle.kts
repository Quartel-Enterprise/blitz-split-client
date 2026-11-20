plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:presentation"))
            api(project(":core:model"))
            api(project(":core:navigation"))
            // Why: Divisão padrão's exceptions name categories (core:expenses) and keep CategorySplit (core:groups).
            api(project(":core:groups"))
            api(project(":core:expenses"))
            // Why: the group's photo is prepared as a profile's (PrepareProfilePhoto, ProfilePhotoPicker).
            implementation(project(":core:profile"))
            implementation(project(":core:settings"))
            implementation(project(":core:utils"))
            implementation(libs.quare.connectivity)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:groups:testing"))
            implementation(project(":core:expenses:testing"))
            implementation(project(":core:profile:testing"))
        }
    }
}
