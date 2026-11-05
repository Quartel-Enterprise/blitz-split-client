plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    // Why: the permission it reads is the shared NotificationPermission, which the ViewModels take as it is.
    api(projects.core.model)
    implementation(projects.core.provider.supabase)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.mlkitVision)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)
    implementation(libs.play.services.code.scanner)
    implementation(libs.play.services.mlkit.barcodeScanning)
}
