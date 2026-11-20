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
        // "37.1" is API level 37 with its first minor release, as androidApp reads it.
        val (api, minor) = libs.versions.android.compileSdk
            .get()
            .split('.')
            .map(String::toInt)
            .let { it[0] to it.getOrElse(1) { 0 } }
        version = release(api) { minorApiLevel = minor }
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
