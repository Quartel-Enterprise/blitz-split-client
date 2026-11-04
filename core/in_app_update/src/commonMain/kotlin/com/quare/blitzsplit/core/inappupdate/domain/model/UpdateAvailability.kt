package com.quare.blitzsplit.core.inappupdate.domain.model

sealed interface UpdateAvailability {
    // Why: what is worth offering: a newer version in the store, or one Google Play already downloaded.
    sealed interface Pending : UpdateAvailability

    data class Available(
        // Why: the App Store says which version it has; Google Play only a version code, so null there.
        val versionName: String?,
        // Why: where "Abrir na App Store" goes; null on Android, whose update runs inside the app.
        val storeUrl: String?,
    ) : Pending

    data object Downloaded : Pending

    // Why: Google Play is already downloading it (the progress shows at the bottom of every screen).
    data object Downloading : UpdateAvailability

    data object NotAvailable : UpdateAvailability

    data object CheckFailed : UpdateAvailability
}
