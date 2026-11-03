package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

// Why: the Google or Apple account the name and photo may come from, and its own picture once the session was read.
internal data class AccountPhoto(
    val provider: AccountProvider?,
    val photoUrl: String?,
)

/*
 * Why: "Editar perfil" and the photo viewer offer the account's picture the same way. The provider comes at once, so
 * the notice never waits on the session; the picture follows once it is read. Keyed on the user too, so another
 * account of the same provider reads its own picture, and a newer status drops a session read still running.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun observeAccountPhoto(
    observeAccount: ObserveAccount,
    getSessionUser: GetSessionUser,
): Flow<AccountPhoto> = observeAccount()
    .map { account -> account.status as? AccountStatus.Connected }
    .distinctUntilChanged()
    .transformLatest { connected ->
        emit(
            AccountPhoto(
                provider = connected?.provider,
                photoUrl = null,
            ),
        )
        // Why: only a connected account has a picture of its own to offer.
        val photoUrl = connected?.let { getSessionUser()?.photoUrl } ?: return@transformLatest
        emit(
            AccountPhoto(
                provider = connected.provider,
                photoUrl = photoUrl,
            ),
        )
    }
