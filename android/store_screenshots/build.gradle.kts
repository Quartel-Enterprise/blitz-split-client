plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

// The store listing screenshots (docs/store-listing-screenshots.md). Nothing ships from here: the generators are
// the unit tests, which draw the real Compose screens from the sample trip inside the store-screenshots frames
// under Robolectric (Google Play), and frame the screens the iOS UI tests captured on the simulator (App Store).
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    testImplementation(projects.shared)
    testImplementation(projects.core.model)
    testImplementation(projects.core.expenses)
    testImplementation(projects.core.activity)
    testImplementation(projects.core.groups)
    testImplementation(projects.android.platform)
    testImplementation(projects.android.feature.groupDetail)
    testImplementation(projects.feature.groupDetail)
    testImplementation(projects.android.feature.splitBill)
    testImplementation(projects.feature.splitBill)
    testImplementation(projects.android.feature.globalBalances)
    testImplementation(projects.feature.globalBalances)
    testImplementation(projects.android.feature.registerPayment)
    testImplementation(projects.feature.registerPayment)
    testImplementation(projects.android.feature.invite)
    testImplementation(projects.feature.invite)
    testImplementation(projects.android.feature.groupStats)
    testImplementation(projects.feature.groupStats)
    testImplementation(projects.android.feature.groups)
    testImplementation(projects.feature.groups)
    testImplementation(projects.android.feature.billDetail)
    testImplementation(projects.feature.billDetail)
    testImplementation(projects.android.feature.activity)
    testImplementation(projects.feature.activity)
    testImplementation(libs.quare.loadable)
    testImplementation(libs.coil.compose)
    testImplementation(libs.storeScreenshots.library)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.compose.ui.testJunit4)
    testImplementation(libs.compose.ui.testManifest)
}

// The Play shots render anywhere; the App Store ones frame the iOS captures, so only a build that asked for them
// runs the iPhone and iPad classes (see storeScreenshots in the root build.gradle.kts).
tasks.withType<Test>().configureEach {
    val outputDir = rootProject.layout.buildDirectory.dir("outputs/store-screenshots").get().asFile
    val capturesDir = rootProject.layout.buildDirectory.dir("outputs/apple-screenshot-captures").get().asFile
    systemProperty("storeScreenshots.outputRoot", outputDir.absolutePath)
    systemProperty("appleScreenshots.capturesDir", capturesDir.absolutePath)
    systemProperty("storeScreenshots.facesDir", rootProject.file("scripts/store-screenshots/faces").absolutePath)
    systemProperty("roborazzi.test.record", "true")
    outputs.dir(outputDir)
    maxHeapSize = "4g"
}
