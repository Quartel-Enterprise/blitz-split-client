plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

/*
 * The Wear OS app (docs/product/watch.md): a standalone watch app with its own session, Room database and sync, over
 * the same shared modules as the phone. It keeps the phone's applicationId, as Google Play's Wear OS track asks, so
 * the two talk over the Data Layer, which only links apps of one id signed by one key.
 *
 * Why the version code offset: Play needs a version code no APK of the listing used before, and the phone's grows by one each release, so the
 * watch's sits a million above it: the watch of one release never takes the phone's code of the next.
 */
val wearVersionCodeOffset = 1_000_000

val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

// Why: the watch's own pushes (FCM), from the same Firebase project as the phone; without it, no pushes.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(
        plugin = libs.plugins.google.services
            .get()
            .pluginId,
    )
}

android {
    namespace = "com.quare.blitzsplit.wear"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        applicationId = "com.quare.blitzsplit"
        // Why: Wear OS 3, the first with Tiles and complications as drawn here (docs/product/general.md).
        minSdk = 30
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = project.property("versionCode").toString().toInt() + wearVersionCodeOffset
        versionName = project.property("versionName").toString()
        buildConfigField("boolean", "HAS_FIREBASE", "$hasFirebaseConfig")
    }
    buildFeatures {
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null) {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:profile"))
    implementation(project(":core:expenses"))
    implementation(project(":core:watch"))
    implementation(project(":core:sync"))
    implementation(project(":feature:watch_pairing"))
    implementation(project(":feature:watch_balances"))
    // Why: its MoneyFormatter writes the amounts as the phone does, in the app's language and the group's currency.
    implementation(project(":android:designsystem"))
    implementation(project(":android:push"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.playServices)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodelNavigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.ui)
    implementation(libs.compose.materialIconsExtended)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation3)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.wear.complications.dataSourceKtx)
    implementation(libs.androidx.concurrent.futuresKtx)
    implementation(libs.wear.toolingPreview)
    implementation(libs.compose.ui.toolingPreview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.wear.compose.uiTooling)
    debugImplementation(libs.wear.tiles.toolingPreview)
}
