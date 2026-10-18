plugins {
    alias(libs.plugins.blitzsplit.android.compose)
}

dependencies {
    implementation(projects.core.provider.supabase)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)
    implementation(libs.play.services.code.scanner)
}
