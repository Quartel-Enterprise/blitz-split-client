plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:global_balances"))
    implementation(project(":android:platform"))
}
