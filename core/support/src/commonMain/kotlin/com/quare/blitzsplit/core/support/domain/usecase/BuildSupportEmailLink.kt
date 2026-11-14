package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics

// Why: the mailto link "Open email" hands the platform, with the subject and the diagnostics already written.
fun interface BuildSupportEmailLink {
    operator fun invoke(diagnostics: SupportDiagnostics): String
}
