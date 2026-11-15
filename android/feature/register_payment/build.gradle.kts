plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:register_payment"))
    implementation(project(":android:platform"))
}
