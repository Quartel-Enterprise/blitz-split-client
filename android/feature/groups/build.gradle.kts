plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:groups"))
    // Why: Início's notification card reads, asks and opens the settings of the notification permission.
    implementation(project(":android:platform"))
}
