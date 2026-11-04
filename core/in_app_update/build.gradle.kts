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
            implementation(projects.core.navigation)
            implementation(projects.core.settings)
            implementation(projects.core.utils)
            implementation(projects.core.provider.analytics)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.play.app.update)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

// Why: the installed version, compared with the App Store's; version.properties is the one source for every platform.
buildkonfig {
    packageName = "com.quare.blitzsplit.core.inappupdate.generated"
    objectName = "InAppUpdateConfig"
    exposeObjectWithName = "InAppUpdateConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", project.property("versionName").toString())
    }
}
