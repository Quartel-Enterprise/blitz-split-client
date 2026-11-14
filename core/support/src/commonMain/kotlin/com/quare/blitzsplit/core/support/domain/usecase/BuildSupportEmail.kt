package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportEmail

// Why: the email "Open email" hands the platform, with the subject and the diagnostics already written.
fun interface BuildSupportEmail {
    operator fun invoke(diagnostics: SupportDiagnostics): SupportEmail
}
