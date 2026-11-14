import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(projects.core.utils)
            implementation(projects.core.settings)
            // Why: OpenLegalPage opens the legal texts' screen.
            implementation(projects.core.navigation)
            implementation(projects.core.provider.room)
            implementation(projects.core.provider.supabase)
            implementation(projects.core.provider.image)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.profile.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Why: which stores list the phone app, for "Get the app" (page 14), from stores.properties (the root build reads it).
val appStoreId = project.property("appStoreId").toString()
require(appStoreId.all(Char::isDigit)) { "stores.properties: appStoreId is the App Store's numeric Apple ID, or empty" }

buildkonfig {
    packageName = "com.quare.blitzsplit.core.profile.generated"
    objectName = "StoreConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.BOOLEAN, "GOOGLE_PLAY", (project.property("googlePlay") == "true").toString())
        buildConfigField(FieldSpec.Type.STRING, "APP_STORE_ID", appStoreId)
    }
}
