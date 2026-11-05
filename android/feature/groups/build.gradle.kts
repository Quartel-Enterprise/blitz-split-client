plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.groups)
    // Why: Início's notification card reads, asks and opens the settings of the notification permission.
    implementation(projects.android.platform)
}
