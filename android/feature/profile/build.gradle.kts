plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.profile)
    implementation(projects.android.platform)
}
