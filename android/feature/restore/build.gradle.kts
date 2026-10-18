plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.restore)
    implementation(projects.android.platform)
}
