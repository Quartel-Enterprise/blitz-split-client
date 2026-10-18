plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.welcome)
    implementation(projects.android.platform)
}
