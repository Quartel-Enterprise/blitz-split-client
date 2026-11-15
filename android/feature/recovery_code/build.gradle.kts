plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:recovery_code"))
    implementation(project(":android:platform"))
}
