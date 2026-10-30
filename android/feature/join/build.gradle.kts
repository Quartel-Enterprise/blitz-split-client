plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.android.platform)
    implementation(projects.feature.join)
}
