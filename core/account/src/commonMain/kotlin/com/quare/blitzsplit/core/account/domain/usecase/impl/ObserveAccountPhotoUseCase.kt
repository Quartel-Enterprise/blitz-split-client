package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.model.AccountPhoto
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountPhoto
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/*
 * Why: the provider comes at once, so the account's notice never waits on the session; the picture follows once it
 * is read. Keyed on the user too, so another account of the same provider reads its own picture, and a newer status
 * drops a session read still running.
 */
class ObserveAccountPhotoUseCase(
    private val observeAccount: ObserveAccount,
    private val getSessionUser: GetSessionUser,
) : ObserveAccountPhoto {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<AccountPhoto> = observeAccount()
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
}
