plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:contacts"))
    implementation(project(":android:platform"))
}
