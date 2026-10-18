package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation

fun interface ValidateDisplayName {
    operator fun invoke(displayName: String): DisplayNameValidation
}
