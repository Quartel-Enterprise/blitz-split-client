package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveAddPersonRequest {
    // Why: the group whose "Add person" is still to open, null while none is.
    operator fun invoke(): Flow<String?>
}
