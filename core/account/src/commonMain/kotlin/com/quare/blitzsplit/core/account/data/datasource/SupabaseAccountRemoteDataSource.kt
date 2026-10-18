package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.account.data.dto.RecoverRequestDto
import com.quare.blitzsplit.core.account.data.dto.RecoverResponseDto
import com.quare.blitzsplit.core.account.data.dto.RecoveryCodeResponseDto
import com.quare.blitzsplit.core.account.domain.model.IdentityAlreadyLinkedException
import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.RecoveryCredentials
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.model.SocialAccountHasNoRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.TooManyRecoveryAttemptsException
import com.quare.blitzsplit.core.model.AccountProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.IDTokenProvider
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAccountRemoteDataSource(
    private val auth: Auth,
    private val functions: Functions,
) : AccountRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeSessionUser(): Flow<SessionUser?> = auth.sessionStatus
        .filter { status -> status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated }
        .map { status -> (status as? SessionStatus.Authenticated)?.session?.user?.let(::toSessionUser) }
        .distinctUntilChanged()

    override suspend fun getCurrentUserOrNull(): SessionUser? {
        auth.awaitInitialization()
        return auth.currentUserOrNull()?.let(::toSessionUser)
    }

    override suspend fun signInAnonymously(): SessionUser {
        auth.signInAnonymously()
        return requireNotNull(auth.currentUserOrNull()?.let(::toSessionUser))
    }

    override suspend fun linkIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ) {
        try {
            auth.linkIdentityWithIdToken(provider = provider.toIdTokenProvider(), idToken = idToken) {
                nonce = rawNonce
            }
            auth.retrieveUserForCurrentSession(updateSession = true)
        } catch (exception: AuthRestException) {
            if (exception.errorCode == AuthErrorCode.IdentityAlreadyExists) throw IdentityAlreadyLinkedException()
            throw exception
        }
    }

    override suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ) {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = provider.toIdTokenProvider()
            nonce = rawNonce
        }
    }

    override suspend fun linkWithBrowser(provider: AccountProvider) {
        auth.linkIdentity(provider.toIdTokenProvider())
    }

    override suspend fun signInWithBrowser(provider: AccountProvider) {
        auth.signInWith(provider.toIdTokenProvider())
    }

    override fun readBrowserCallback(url: String): BrowserCallback? = SupabaseCallbackUrl.read(url)

    override suspend fun signInWithPassword(credentials: RecoveryCredentials) {
        auth.signInWith(Email) {
            email = credentials.email
            password = credentials.password
        }
    }

    override suspend fun fetchRecoveryCredentials(code: String): RecoveryCredentials {
        val response = try {
            functions.invoke(function = RECOVER_FUNCTION, body = RecoverRequestDto(code = code))
        } catch (exception: RestException) {
            throw when (exception.statusCode) {
                HTTP_BAD_REQUEST, HTTP_NOT_FOUND -> InvalidRecoveryCodeException()
                HTTP_TOO_MANY_REQUESTS -> TooManyRecoveryAttemptsException()
                else -> exception
            }
        }
        val dto = json.decodeFromString<RecoverResponseDto>(response.bodyAsText())
        return RecoveryCredentials(email = dto.email, password = dto.password)
    }

    override suspend fun createRecoveryCode(): String {
        val response = try {
            functions.invoke(function = RECOVERY_CODE_FUNCTION)
        } catch (exception: RestException) {
            if (exception.statusCode == HTTP_CONFLICT) throw SocialAccountHasNoRecoveryCodeException()
            throw exception
        }
        return json.decodeFromString<RecoveryCodeResponseDto>(response.bodyAsText()).code
    }

    private fun toSessionUser(user: UserInfo): SessionUser = SessionUser(
        userId = user.id,
        socialProvider = user.identities.orEmpty().firstNotNullOfOrNull { identity ->
            AccountProvider.fromCodeOrNull(identity.provider)
        },
        displayName = user.userMetadata.findString("full_name", "name"),
        photoUrl = user.userMetadata.findString("avatar_url", "picture"),
    )

    private fun JsonObject?.findString(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        this
            ?.get(key)
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
    }

    private fun AccountProvider.toIdTokenProvider(): IDTokenProvider = when (this) {
        AccountProvider.GOOGLE -> Google
        AccountProvider.APPLE -> Apple
    }

    private companion object {
        const val RECOVER_FUNCTION = "recover"
        const val RECOVERY_CODE_FUNCTION = "recovery-code"
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
