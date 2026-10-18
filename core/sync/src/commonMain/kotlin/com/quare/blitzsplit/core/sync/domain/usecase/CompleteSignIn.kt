package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome

/*
 * Why: after any sign in (ID token, browser or recovery code) the device pulls what the account
 * already has, so the next screen depends on whether a profile came back. A failed pull fails the
 * whole step: asking for a new profile then would overwrite the one on the server.
 */
fun interface CompleteSignIn {
    suspend operator fun invoke(): Result<SignInOutcome>
}
