plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:restore"))
    implementation(project(":android:platform"))
}
