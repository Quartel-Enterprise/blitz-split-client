import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            // Why: ClientPlatform, which the diagnostics name, and the profile's link.
            implementation(projects.core.profile)
            implementation(projects.core.account)
            implementation(projects.core.settings)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Why: the installed version, which a support email names; version.properties is the one source for every platform.
buildkonfig {
    packageName = "com.quare.blitzsplit.core.support.generated"
    objectName = "SupportConfig"
    exposeObjectWithName = "SupportConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", project.property("versionName").toString())
    }
}
