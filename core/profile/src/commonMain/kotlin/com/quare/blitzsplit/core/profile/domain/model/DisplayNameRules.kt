package com.quare.blitzsplit.core.profile.domain.model

// Why: the same bounds as the check constraint of public.profiles in the API.
object DisplayNameRules {
    const val MIN_LENGTH: Int = 2
    const val MAX_LENGTH: Int = 40
}
