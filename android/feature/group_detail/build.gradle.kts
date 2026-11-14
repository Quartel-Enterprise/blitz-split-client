plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.feature.groupDetail)
    // Why: "Export group data" hands its file to the system's save sheet.
    implementation(projects.android.platform)
}
