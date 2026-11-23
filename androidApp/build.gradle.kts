import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.firebase.crashlytics)
}

/*
 * Populated only by the release workflow, which exports it after decoding the keystore from GitHub
 * secrets. Every other release build is a compile check nobody ships.
 */
val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

/*
 * Firebase (Atividade's push notifications and Crashlytics): the Firebase project's google-services.json, git-ignored,
 * next to this file. Without it the app never starts Firebase, so a build without it, CI's included, runs with no
 * notifications nor crash reports. The google-services plugin turns it into the resources Firebase starts from, and
 * Crashlytics needs that plugin to upload R8's mapping.
 */
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(
        plugin = libs.plugins.google.services
            .get()
            .pluginId,
    )
}

/*
 * Firebase Analytics collects in release builds, on by default under legitimate interest, until Profile's "Share usage
 * data" turns it off on the device, which the SDK then keeps over the manifest (docs/analytics/README.md#consent).
 * false turns it off for everyone.
 */
val collectsAnalytics = true

android {
    namespace = "com.quare.blitzsplit"
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
        // The legacy app's id: this app replaces it in the Play Store
        applicationId = "com.quare.blitzsplit"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = project.property("versionCode").toString().toInt()
        versionName = project.property("versionName").toString()
        buildConfigField("boolean", "HAS_FIREBASE", "$hasFirebaseConfig")
        // Why: checked before attaching Analytics, whose runtime setting would override the manifest's.
        buildConfigField("boolean", "COLLECTS_ANALYTICS", "$collectsAnalytics")
    }
    buildFeatures {
        buildConfig = true
    }
    // Lists English, Portuguese (pt and pt-PT), Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish,
    // Indonesian (values-in, Android's legacy code for id), Russian, Ukrainian, Korean, Hindi, Arabic, Simplified
    // Chinese (values-b+zh+Hans), Hebrew (values-iw, Android's legacy code for he), Thai, Vietnamese, Traditional
    // Chinese (values-b+zh+Hant), Swedish, Czech, Romanian, Bengali, Malay, Greek, Croatian, Hungarian, Urdu, Filipino, Serbian and Persian in the system's per-app language setting (Android 13+)
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
        // Firebase Analytics never collects from a debug build, from its first launch on
        getByName("debug") {
            manifestPlaceholders["analyticsCollectionEnabled"] = false
        }
        getByName("release") {
            manifestPlaceholders["analyticsCollectionEnabled"] = collectsAnalytics
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = hasFirebaseConfig
            }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:account"))
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
    implementation(project(":core:provider:supabase"))
    implementation(project(":core:profile"))
    implementation(project(":core:provider:crashlytics"))
    implementation(project(":core:provider:analytics"))
    implementation(project(":core:sync"))
    implementation(project(":core:watch"))
    implementation(project(":core:in_app_update"))
    implementation(project(":android:platform"))
    implementation(project(":android:push"))

    // Android UI
    implementation(project(":android:designsystem"))
    implementation(project(":android:feature:welcome"))
    implementation(project(":android:feature:profile_setup"))
    implementation(project(":android:feature:restore"))
    implementation(project(":android:feature:recovery_code"))
    implementation(project(":android:feature:groups"))
    implementation(project(":android:feature:create_group"))
    implementation(project(":android:feature:invite"))
    implementation(project(":android:feature:members"))
    implementation(project(":android:feature:join"))
    implementation(project(":android:feature:group_detail"))
    implementation(project(":android:feature:split_bill"))
    implementation(project(":android:feature:bill_detail"))
    implementation(project(":android:feature:edit_group"))
    implementation(project(":android:feature:register_payment"))
    implementation(project(":android:feature:global_balances"))
    implementation(project(":android:feature:activity"))
    implementation(project(":android:feature:contacts"))
    implementation(project(":android:feature:group_stats"))
    implementation(project(":android:feature:profile"))
    implementation(project(":android:feature:edit_profile"))
    implementation(project(":android:feature:payment_key"))
    implementation(project(":android:feature:paying_method"))
    implementation(project(":android:feature:delete_account"))
    implementation(project(":android:feature:connect_watch"))
    implementation(project(":android:feature:contact_support"))
    implementation(project(":android:feature:notification_settings"))
    implementation(project(":android:feature:category_map"))
    implementation(project(":android:feature:in_app_update"))
    implementation(project(":android:feature:legal"))
    implementation(project(":android:feature:release_notes"))

    // Atividade's push notifications (Crashlytics comes with :core:provider:crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // The paired watch: its pairing code and the sign out it follows (docs/product/watch.md)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.playServices)

    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // Compose and Navigation 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    // Why: real time listens while the app is in front, a rotation or a second activity not counting as leaving it
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodelNavigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation3)
    implementation(libs.compose.ui)
}
