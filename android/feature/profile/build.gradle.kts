plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:profile"))
    implementation(project(":android:platform"))
    implementation(libs.androidx.core.ktx)
}
