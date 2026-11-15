plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":android:platform"))
    implementation(project(":feature:create_group"))
}
