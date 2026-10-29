package com.quare.blitzsplit.core.sync.signin

import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

/*
 * Why: every screen that signs in with Google or Apple (the welcome, the restore) walks the same sign in, so a
 * fix to the native or browser path reaches all of them. Each screen keeps its own state, guards, error and
 * navigation, reads the outcome from a SignInStep, and launches these calls in its own scope.
 */
interface ProviderSignIn {
    // Why: fails only when the browser sign in couldn't start; a native one goes on in the platform's sheet.
    suspend fun signIn(
        provider: AccountProvider,
        launchNativeSignIn: suspend (AccountProvider) -> Unit,
    ): Result<Unit>

    suspend fun onIdTokenReceived(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
        authorizationCode: String?,
    ): SignInStep

    // Why: a sign in the screen completes itself (the recovery code); fails with signIn's own error.
    suspend fun signInDirectly(signIn: suspend () -> Result<*>): Result<SignInStep>

    fun observeBrowserSignIn(
        isShown: () -> Boolean,
        onStep: (SignInStep) -> Unit,
    ): Flow<Unit>
}
