package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability

fun interface CheckForUpdate {
    suspend operator fun invoke(): UpdateAvailability
}
