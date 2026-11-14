package com.quare.blitzsplit.core.legal.domain.usecase

/*
 * Why: at launch and when Profile › Language changes, both legal texts are brought up to date in the language they
 * are read in, so Profile's footer opens them offline too. A failure is left for the screen, which tries again.
 */
fun interface PrefetchLegalDocuments {
    suspend operator fun invoke()
}
