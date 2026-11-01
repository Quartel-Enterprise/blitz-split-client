plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.globalBalances)
    implementation(projects.android.platform)
}
