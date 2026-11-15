plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:activity"))
    implementation(project(":android:platform"))
    implementation(libs.coil.compose)
}
