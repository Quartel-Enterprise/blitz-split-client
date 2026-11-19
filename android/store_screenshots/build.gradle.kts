import com.quare.blitzsplit.buildlogic.configureStoreScreenshots

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
    testImplementation(project(":shared"))
    testImplementation(project(":core:model"))
    testImplementation(project(":core:expenses"))
    testImplementation(project(":core:activity"))
    testImplementation(project(":core:groups"))
    testImplementation(project(":core:profile"))
    testImplementation(project(":android:platform"))
    testImplementation(project(":android:feature:group_detail"))
    testImplementation(project(":feature:group_detail"))
    testImplementation(project(":android:feature:split_bill"))
    testImplementation(project(":feature:split_bill"))
    testImplementation(project(":android:feature:global_balances"))
    testImplementation(project(":feature:global_balances"))
    testImplementation(project(":android:feature:register_payment"))
    testImplementation(project(":feature:register_payment"))
    testImplementation(project(":android:feature:invite"))
    testImplementation(project(":feature:invite"))
    testImplementation(project(":android:feature:group_stats"))
    testImplementation(project(":feature:group_stats"))
    testImplementation(project(":android:feature:groups"))
    testImplementation(project(":feature:groups"))
    testImplementation(project(":android:feature:bill_detail"))
    testImplementation(project(":feature:bill_detail"))
    testImplementation(project(":android:feature:activity"))
    testImplementation(project(":feature:activity"))
    testImplementation(libs.quare.loadable)
    testImplementation(libs.coil.compose)
    testImplementation(libs.storeScreenshots.library)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.compose.ui.testJunit4)
    testImplementation(libs.compose.ui.testManifest)
}

configureStoreScreenshots()
