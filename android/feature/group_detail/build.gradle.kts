plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(project(":feature:group_detail"))
    // Why: "Export group data" hands its file to the system's save sheet.
    implementation(project(":android:platform"))
}
