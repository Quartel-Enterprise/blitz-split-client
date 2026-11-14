plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    api(projects.core.model)
    // Why: the balance card, the payment notice and the method names draw core:expenses' models.
    implementation(projects.core.expenses)
    // Why: previews take their initials from core:groups' Initials, the rule every ViewModel uses.
    implementation(projects.core.groups)
    api(libs.androidx.graphics.shapes)
    api(libs.compose.materialIconsExtended)
    // Why: the fold the device reports (androidx.window's FoldingFeature, through Material's adaptive reader).
    implementation(libs.compose.material3.adaptive)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
