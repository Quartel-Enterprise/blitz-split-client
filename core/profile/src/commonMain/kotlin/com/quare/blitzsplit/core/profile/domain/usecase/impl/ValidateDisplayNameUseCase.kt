package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName

class ValidateDisplayNameUseCase : ValidateDisplayName {
    override fun invoke(displayName: String): DisplayNameValidation {
        val length = displayName.trim().length
        return when {
            length < DisplayNameRules.MIN_LENGTH -> DisplayNameValidation.TOO_SHORT
            length > DisplayNameRules.MAX_LENGTH -> DisplayNameValidation.TOO_LONG
            else -> DisplayNameValidation.VALID
        }
    }
}
