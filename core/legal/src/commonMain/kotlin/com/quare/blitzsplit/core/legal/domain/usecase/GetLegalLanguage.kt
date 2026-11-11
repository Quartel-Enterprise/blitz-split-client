package com.quare.blitzsplit.core.legal.domain.usecase

/*
 * Why: the language the legal texts are asked for in, as the site names them ("pt", "tr"): Profile › Language's, or
 * the device's own while it follows the system, since the site writes them in languages the app doesn't speak.
 */
fun interface GetLegalLanguage {
    suspend operator fun invoke(): String
}
