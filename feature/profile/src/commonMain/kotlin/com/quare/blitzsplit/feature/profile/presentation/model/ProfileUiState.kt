package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference

data class ProfileUiState(
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
    // Why: Pix first, each method's default first, as the settle screen offers them.
    val paymentKeys: List<PaymentKeyItem>,
    val isSignOutScheduled: Boolean,
    val theme: ThemePreference,
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
) {
    val avatarInitial: String
        get() = displayName
            .trim()
            .firstOrNull()
            ?.uppercase()
            .orEmpty()

    val hasAccount: Boolean
        get() = accountProvider != null

    // Why: only an anonymous user that synced has a code; a local only one has nothing to recover with.
    val showsRecoveryCodeRow: Boolean
        get() = !hasAccount && hasRecoveryCode

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
        get() = isOnline && connectingProvider == null && !isSigningOut

    // Why: "Conectar" and "Conectar Google ou Apple em vez disso" only open the provider buttons, which need the internet.
    val canConnect: Boolean
        get() = isOnline && !isSigningOut

    val showsConnectButtons: Boolean
        get() = !hasAccount && isConnectExpanded

    // Why: a link half done would land on a device whose session and profile are already gone.
    val canSignOut: Boolean
        get() = !isSigningOut && !isSignOutScheduled && connectingProvider == null

    val canErase: Boolean
        get() = hasConfirmedCodeSaved && !isSigningOut
}
