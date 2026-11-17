plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    api(project(":core:model"))
    // Why: the balance card, the payment notice and the method names draw core:expenses' models.
    implementation(project(":core:expenses"))
    /*
     * Why: previews take their initials from core:groups' Initials, the rule every ViewModel uses, and the group
     * reminder's section, schedule texts and calendar launcher take its GroupReminder and ReminderCalendarEvent.
     */
    api(project(":core:groups"))
    api(libs.androidx.graphics.shapes)
    api(libs.compose.materialIconsExtended)
    // Why: the fold the device reports (androidx.window's FoldingFeature, through Material's adaptive reader).
    implementation(libs.compose.material3.adaptive)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
