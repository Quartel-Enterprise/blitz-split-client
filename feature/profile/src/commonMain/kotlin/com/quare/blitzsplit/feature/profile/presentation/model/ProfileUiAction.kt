package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface ProfileUiAction {
    // Why: the system share sheet with the profile link (the Mac's sharing picker, navigator.share or a copy on the web).
    data class ShareProfileLink(
        val link: String,
        val name: String,
    ) : ProfileUiAction

    // Why: "Link copied".
    data class CopyProfileLink(
        val link: String,
    ) : ProfileUiAction

    data class CopyToClipboard(
        val formattedCode: String,
    ) : ProfileUiAction

    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : ProfileUiAction

    // Why: each platform saves it its own way (a save sheet, a download), in a file named in its own words.
    data class SaveExport(
        val json: String,
    ) : ProfileUiAction

    // Why: each platform opens the page its own way: a Custom Tab, Safari, or a new browser tab.
    data class OpenUrl(
        val url: String,
    ) : ProfileUiAction

    // Why: "Buscar atualizações" with nothing to offer, said in a snackbar, a toast or a banner.
    data class ShowUpdateCheckResult(
        val result: UpdateCheckResult,
    ) : ProfileUiAction

    /*
     * Why: a language just picked; Android before 13 opens its screen again in it and the web reloads the page, while
     * Android 13 and later and the Apple apps already redraw in it.
     */
    data object ApplyLanguage : ProfileUiAction
}
