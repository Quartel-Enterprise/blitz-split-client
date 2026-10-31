plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    api(projects.core.model)
    // Why: the balance card, the payment notice and the method names draw core:expenses' models.
    implementation(projects.core.expenses)
    api(libs.androidx.graphics.shapes)
    api(libs.compose.materialIconsExtended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
