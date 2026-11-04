package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.model.UpdatePromptSource

fun interface ShowUpdatePrompt {
    suspend operator fun invoke(
        availability: UpdateAvailability.Pending,
        source: UpdatePromptSource,
    )
}
