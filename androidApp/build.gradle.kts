plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

/*
 * Populated only by the release workflow, which exports it after decoding the keystore from GitHub
 * secrets. Every other release build is a compile check nobody ships.
 */
val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.quare.blitzsplit"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
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
    }
    // Lists English and Portuguese in the system's per-app language setting (Android 13+)
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
    implementation(projects.shared)
    implementation(projects.core.account)
    implementation(projects.core.navigation)
    implementation(projects.core.model)
    implementation(projects.core.provider.supabase)

    // Android UI
    implementation(projects.android.designsystem)
    implementation(projects.android.feature.welcome)
    implementation(projects.android.feature.profileSetup)
    implementation(projects.android.feature.restore)
    implementation(projects.android.feature.recoveryCode)
    implementation(projects.android.feature.groups)
    implementation(projects.android.feature.createGroup)
    implementation(projects.android.feature.invite)
    implementation(projects.android.feature.members)
    implementation(projects.android.feature.join)
    implementation(projects.android.feature.groupDetail)
    implementation(projects.android.feature.splitBill)
    implementation(projects.android.feature.billDetail)
    implementation(projects.android.feature.editGroup)
    implementation(projects.android.feature.registerPayment)
    implementation(projects.android.feature.globalBalances)
    implementation(projects.android.feature.groupStats)
    implementation(projects.android.feature.profile)
    implementation(projects.android.feature.editProfile)
    implementation(projects.android.feature.paymentKey)
    implementation(projects.android.feature.deleteAccount)
    implementation(projects.android.feature.notificationSettings)
    implementation(projects.android.feature.categoryMap)

    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // Compose and Navigation 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodelNavigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation3)
    implementation(libs.compose.ui)
}
