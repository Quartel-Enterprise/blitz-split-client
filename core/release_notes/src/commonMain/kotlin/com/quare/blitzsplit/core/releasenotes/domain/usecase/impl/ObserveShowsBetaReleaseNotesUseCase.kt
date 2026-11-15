package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetInstalledVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveShowsBetaReleaseNotes
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBetaReleaseNotesChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveShowsBetaReleaseNotesUseCase(
    private val observeBetaReleaseNotesChoice: ObserveBetaReleaseNotesChoice,
    private val getInstalledVersion: GetInstalledVersion,
) : ObserveShowsBetaReleaseNotes {
    override fun invoke(): Flow<Boolean> {
        val isBetaBuild = getInstalledVersion()?.isBeta == true
        return observeBetaReleaseNotesChoice().map { choice -> choice ?: isBetaBuild }
    }
}
