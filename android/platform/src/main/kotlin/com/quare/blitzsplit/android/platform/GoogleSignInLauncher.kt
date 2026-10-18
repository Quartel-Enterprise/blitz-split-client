package com.quare.blitzsplit.android.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.quare.blitzsplit.core.provider.supabase.generated.SupabaseConfig
import kotlinx.coroutines.launch

/*
 * Why: Google comes first on Android through the native Credential Manager sheet, which hands an
 * ID token straight to the shared ViewModel.
 */
@Composable
fun rememberGoogleSignInLauncher(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(context) {
        { scope.launch { currentOnResult.value(requestGoogleIdToken(context)) } }
    }
}

private suspend fun requestGoogleIdToken(context: Context): GoogleSignInResult {
    val rawNonce = createRawNonce()
    val option = GetGoogleIdOption
        .Builder()
        .setServerClientId(SupabaseConfig.SUPABASE_GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .setNonce(rawNonce.toSha256Hex())
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInResult.Success(
                idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken,
                rawNonce = rawNonce,
            )
        } else {
            GoogleSignInResult.Failed
        }
    } catch (cancelled: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (failure: GetCredentialException) {
        GoogleSignInResult.Failed
    } catch (invalidToken: GoogleIdTokenParsingException) {
        GoogleSignInResult.Failed
    }
}
