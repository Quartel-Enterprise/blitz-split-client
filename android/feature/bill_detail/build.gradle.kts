plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.android.platform)
    implementation(projects.feature.billDetail)
    // Why: the bill's photos, a data URI until they are up, then the server's signed URL.
    implementation(libs.coil.compose)
    // Why: the viewer's light system bar icons over its black window.
    implementation(libs.androidx.core.ktx)
}
