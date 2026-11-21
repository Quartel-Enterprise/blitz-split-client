plugins {
    alias(libs.plugins.android.library)
}

/*
 * Activity's push notifications worded on the device, shared by the phone and the watch app: the kinds the server
 * sends, their sentence in each language, and the channel each goes to. No Compose, so the watch takes none of the
 * phone's UI with it.
 */
android {
    namespace = "com.quare.blitzsplit.android.push"
    compileSdk {
        version = release(
            libs.versions.android.compileSdk
                .get()
                .toInt(),
        ) {
            minorApiLevel = libs.versions.android.compileSdkMinor
                .get()
                .toInt()
        }
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
}

dependencies {
    api(project(":core:profile"))
    implementation(libs.androidx.annotation)
}
