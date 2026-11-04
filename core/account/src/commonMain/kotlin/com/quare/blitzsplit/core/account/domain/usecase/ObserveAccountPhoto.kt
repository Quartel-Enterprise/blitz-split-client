package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.AccountPhoto
import kotlinx.coroutines.flow.Flow

// Why: "Editar perfil" and the profile's photo viewer offer the account's own picture the same way.
fun interface ObserveAccountPhoto {
    operator fun invoke(): Flow<AccountPhoto>
}
