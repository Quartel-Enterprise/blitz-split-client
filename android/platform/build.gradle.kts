plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    // Why: the permission it reads is the shared NotificationPermission, which the ViewModels take as it is.
    api(project(":core:model"))
    implementation(project(":core:provider:supabase"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.mlkitVision)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)
    // Why: Google Play's in-app review card.
    implementation(libs.play.review)
    implementation(libs.play.services.code.scanner)
    implementation(libs.play.services.mlkit.barcodeScanning)
}
