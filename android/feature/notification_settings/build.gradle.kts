plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":android:platform"))
    implementation(project(":android:push"))
    implementation(project(":feature:notification_settings"))
}
