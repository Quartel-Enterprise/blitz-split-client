plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.recoveryCode)
    implementation(projects.android.platform)
}
