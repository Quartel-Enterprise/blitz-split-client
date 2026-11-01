plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            // Why: a contact is someone the groups have, with the balance core:expenses works out.
            api(projects.core.groups)
            api(projects.core.expenses)
            implementation(projects.core.utils)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.contacts.testing)
            implementation(projects.core.groups.testing)
            implementation(projects.core.expenses.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
