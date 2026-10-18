package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.SessionUser

fun interface GetSessionUser {
    suspend operator fun invoke(): SessionUser?
}
