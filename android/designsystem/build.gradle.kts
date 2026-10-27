plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.graphics.shapes)
    api(libs.compose.materialIconsExtended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
