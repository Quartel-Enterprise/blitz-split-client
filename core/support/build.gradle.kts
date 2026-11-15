import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            // Why: ClientPlatform, which the diagnostics name, and the profile's link.
            implementation(project(":core:profile"))
            implementation(project(":core:account"))
            implementation(project(":core:settings"))
            // Why: EventAnalytics and TrackEvent, for the event a review request sends.
            implementation(project(":core:presentation"))
            // Why: the review asks only over a resting screen, after a payment that settles a group.
            implementation(project(":core:navigation"))
            implementation(project(":core:expenses"))
            implementation(project(":core:sync"))
            implementation(project(":core:utils"))
            implementation(project(":core:provider:room"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Why: RoomReviewPromptLocalDataSourceTest runs over an in-memory Room on the jvm target.
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

// Why: the installed version, which a support email names and a review is asked once for; version.properties is the one source for every platform.
buildkonfig {
    packageName = "com.quare.blitzsplit.core.support.generated"
    objectName = "SupportConfig"
    exposeObjectWithName = "SupportConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", project.property("versionName").toString())
    }
}
