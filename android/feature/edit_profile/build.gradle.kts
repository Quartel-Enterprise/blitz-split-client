plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":android:platform"))
    implementation(project(":feature:edit_profile"))
}
