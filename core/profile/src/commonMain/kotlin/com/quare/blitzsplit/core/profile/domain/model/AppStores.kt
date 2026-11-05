package com.quare.blitzsplit.core.profile.domain.model

// Why: which stores list the phone app (stores.properties); "Get the app" only shows once one does.
data class AppStores(
    val googlePlay: Boolean,
    // Why: the numeric Apple ID of the App Store listing; null until there is one.
    val appStoreId: String?,
) {
    val appStoreUrl: String?
        get() = appStoreId?.let { "https://apps.apple.com/app/id$it" }
}
