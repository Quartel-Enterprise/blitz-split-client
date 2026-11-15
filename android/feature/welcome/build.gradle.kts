plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:welcome"))
    implementation(project(":android:platform"))
}
