package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded

data class ProfileUiState(
    /*
     * Why: the profile, the account, the theme and what waits to go up are read apart; until each answered, the
     * screen shimmers rather than draw "Sem conta" with Conectar, no name or the default theme.
     */
    val isLoaded: Boolean,
    val displayName: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: null without Google or Apple, both before the first sync and while anonymous.
    val accountProvider: AccountProvider?,
    // Why: a local only user has no ID on the server yet, so the account row can't call it anonymous.
    val hasAnonymousId: Boolean,
    // Why: masked here, so every UI hides the same characters ("p•••@gmail.com").
    val maskedEmail: String?,
    val formattedRecoveryCode: String?,
    // Why: "Ver e guardar" shows the recovery code sheet again, as the groups do while it is due.
    val showsRecoveryCodePrompt: Boolean,
    val isOnline: Boolean,
    val syncStatus: ProfileSyncStatus?,
    val pendingChanges: List<PendingChange>,
    /*
     * Why: Pix first, each method's default first, as the settle screen offers them. Loading until read and while a
     * restored account's copy is not whole, since its keys only come with that sync: never "no key" before it.
     */
    val paymentKeys: Loadable<List<PaymentKeyItem>>,
    val isSignOutScheduled: Boolean,
    val theme: ThemePreference,
    // Why: "Cores dinâmicas" only shows where the platform draws the wallpaper's scheme (Android 12 and later).
    val showsDynamicColors: Boolean,
    val usesDynamicColors: Boolean,
    // Why: Preferências' "Bitcoin em satoshis", every bitcoin amount shown and typed in whole sats on this device.
    val showsBitcoinInSats: Boolean,
    // Why: Preferences' "Language", this device's pick, SYSTEM following the device.
    val language: AppLanguage,
    /*
     * Why: "5 active" under "Notifications", the rows whose push is on; the toggles live on their own screen or section. Loading like
     * the keys, since a restored account's choices only come with its first full sync.
     */
    val activeNotificationCount: Loadable<Int>,
    /*
     * Why: "Check for updates" with the version under it, where a store sends new versions (Android, iOS, macOS); the
     * web always serves the latest, so its version is only the legal footer's.
     */
    val showsCheckForUpdates: Boolean,
    // Why: "Verificando…" under it while the store is asked, which also ignores another tap.
    val isCheckingForUpdates: Boolean,
    val dialog: ProfileDialog?,
    // Why: "Apagar e sair" only unlocks once the person says the recovery code is kept.
    val hasConfirmedCodeSaved: Boolean,
    // Why: "Conectar" opens into one button per provider, as the recovery code's does.
    val isConnectExpanded: Boolean,
    val connectingProvider: AccountProvider?,
    val hasSignInFailed: Boolean,
    val isSigningOut: Boolean,
    // Why: each UI says it in its own words, in the dialog that stays open for another try.
    val hasSignOutFailed: Boolean,
    val isUnlinking: Boolean,
    // Why: like the sign out's, said in the dialog that stays open for another try.
    val hasUnlinkFailed: Boolean,
    // Why: "App on your phone" (14.4), only on the web and the Mac once a store lists the app; null everywhere else.
    val appInvite: ProfileAppInvite?,
) {
    val avatarInitial: String
        get() = displayName
            .trim()
            .firstOrNull()
            ?.uppercase()
            .orEmpty()

    val hasAccount: Boolean
        get() = accountProvider != null

    // Why: "System" first, then each language, which every UI names in the app's current language.
    val languageOptions: List<AppLanguage>
        get() = AppLanguage.entries

    // Why: only an anonymous user that synced has a code; a local only one has nothing to recover with.
    val showsRecoveryCodeRow: Boolean
        get() = isLoaded && !hasAccount && hasRecoveryCode

    val showsOfflineNotice: Boolean
        get() = !isOnline

    val showsProfilePending: Boolean
        get() = PendingChange.PROFILE in pendingChanges && syncStatus != ProfileSyncStatus.SYNCING

    val pendingChangeCount: Int
        get() = pendingChanges.size

    // Why: the erase dialog warns that these are lost, since the recovery code brings back only what went up.
    val hasPendingChanges: Boolean
        get() = pendingChanges.isNotEmpty()

    val hasRecoveryCode: Boolean
        get() = formattedRecoveryCode != null

    val canRetrySync: Boolean
        get() = syncStatus == ProfileSyncStatus.FAILED

    val areAccountButtonsEnabled: Boolean
        get() = isLoaded && isOnline && connectingProvider == null && !isSigningOut

    // Why: "Conectar" and "Conectar Google ou Apple em vez disso" only open the provider buttons, which need the internet.
    val canConnect: Boolean
        get() = isLoaded && isOnline && !isSigningOut

    val showsConnectButtons: Boolean
        get() = isLoaded && !hasAccount && isConnectExpanded

    /*
     * Why: a link half done would land on a device whose session and profile are already gone, and before the read
     * the dialog would be picked from no account and no pending change.
     */
    val canSignOut: Boolean
        get() = isLoaded && !isSigningOut && !isSignOutScheduled && connectingProvider == null && !isUnlinking

    // Why: the dialog opens offline too, to say why "Desconectar" waits for the internet.
    val canUnlink: Boolean
        get() = isOnline && !isUnlinking && !isSigningOut && !isSignOutScheduled

    // Why: a profile the server never saw has no account there to delete (beta feedback 17).
    val isLocalOnly: Boolean
        get() = !hasAccount && !hasAnonymousId

    /*
     * Why: leaving only erases the profile for good when nothing brings it back, neither Google or Apple nor a recovery
     * code, so only then do the button and its dialog say "Erase profile and sign out"; any other account, an
     * anonymous one with its code too, signs out and comes back later (beta feedback 17). An anonymous account whose
     * code isn't made yet is one of them, though the server keeps it and it can be deleted.
     */
    val erasesProfileOnSignOut: Boolean
        get() = !hasAccount && !hasRecoveryCode

    /*
     * Why: "Sign out" and "Delete account" are apart for every account the server keeps, an anonymous one too: the
     * server deletes it. A local only profile has nothing there, and "Erase profile and sign out" already erases it.
     */
    val showsDeleteAccount: Boolean
        get() = isLoaded && !isLocalOnly

    // Why: a key added before the account's own came down could take the default the server already gave another.
    val canAddPaymentKey: Boolean
        get() = paymentKeys.isLoaded()

    val canErase: Boolean
        get() = hasConfirmedCodeSaved && !isSigningOut
}
