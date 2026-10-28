package com.quare.blitzsplit.core.account.data

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.datasource.BrowserCallback
import com.quare.blitzsplit.core.account.data.repository.AccountRepositoryImpl
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.IdentityAlreadyLinkedException
import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.RecoveryCredentials
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.model.SocialAccountHasNoRecoveryCodeException
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryImplTest {
    private lateinit var local: FakeAccountLocalDataSource
    private lateinit var remote: FakeAccountRemoteDataSource
    private lateinit var repository: AccountRepositoryImpl

    @Test
    fun `GIVEN no session WHEN ensuring registration THEN signs in anonymously and stores the user`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.ensureRegistered()

        // Then
        assertEquals("anonymous", result.getOrNull())
        assertEquals(1, remote.anonymousSignIns)
        assertEquals(AccountStatus.Anonymous("anonymous"), repository.observeStatus().first())
    }

    @Test
    fun `GIVEN a session WHEN ensuring registration THEN reuses it`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = repository.ensureRegistered()

        // Then
        assertEquals("anonymous", result.getOrNull())
        assertEquals(0, remote.anonymousSignIns)
    }

    @Test
    fun `GIVEN an anonymous user WHEN connecting Google THEN links the identity`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.GOOGLE,
            idToken = "token",
            rawNonce = "nonce",
            fullName = null,
        )

        // Then
        assertEquals(ConnectResult.LINKED, result.getOrNull())
        assertEquals(listOf("token"), remote.linkedTokens)
        assertTrue(remote.signedInTokens.isEmpty())
    }

    @Test
    fun `GIVEN the identity belongs to another user WHEN connecting THEN signs in to that user`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser, linkError = IdentityAlreadyLinkedException())

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.APPLE,
            idToken = "token",
            rawNonce = null,
            fullName = null,
        )

        // Then
        assertEquals(ConnectResult.SWITCHED, result.getOrNull())
        assertEquals(listOf("token"), remote.signedInTokens)
    }

    @Test
    fun `GIVEN no session WHEN connecting THEN signs in with the token`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.GOOGLE,
            idToken = "token",
            rawNonce = null,
            fullName = null,
        )

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertTrue(remote.linkedTokens.isEmpty())
    }

    @Test
    fun `GIVEN a Google session WHEN ensuring registration THEN the status is connected and the prompt settled`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            remote.sessionUsers.value = SessionUser("user-1", AccountProvider.GOOGLE, "Pierre", null)

            // When
            repository.ensureRegistered()

            // Then
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), repository.observeStatus().first())
            assertEquals(RecoveryCodePrompt.SAVED, repository.observeRecoveryCodePrompt().first())
        }

    @Test
    fun `GIVEN an anonymous user WHEN issuing a recovery code THEN stores it and makes the prompt due`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = repository.createRecoveryCode()

        // Then
        assertEquals("K7QM4XPA9TRE", result.getOrNull())
        assertEquals("K7QM4XPA9TRE", repository.observeRecoveryCode().first())
        assertEquals(RecoveryCodePrompt.DUE, repository.observeRecoveryCodePrompt().first())
    }

    @Test
    fun `GIVEN a social account WHEN issuing a recovery code THEN settles the prompt`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser, issueError = SocialAccountHasNoRecoveryCodeException())

        // When
        val result = repository.createRecoveryCode()

        // Then
        assertIs<SocialAccountHasNoRecoveryCodeException>(result.exceptionOrNull())
        assertEquals(RecoveryCodePrompt.SAVED, repository.observeRecoveryCodePrompt().first())
    }

    @Test
    fun `GIVEN a mistyped code WHEN restoring THEN fails without calling the server`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.restoreWithRecoveryCode("K7QM-4XPA")

        // Then
        assertIs<InvalidRecoveryCodeException>(result.exceptionOrNull())
        assertTrue(remote.exchangedCodes.isEmpty())
    }

    @Test
    fun `GIVEN a valid code WHEN restoring THEN signs in with the credentials and keeps the code as saved`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.restoreWithRecoveryCode("k7qm-4xpa-9tre")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("K7QM4XPA9TRE"), remote.exchangedCodes)
        assertEquals(AccountStatus.Anonymous("restored"), repository.observeStatus().first())
        assertEquals(RecoveryCodePrompt.SAVED, repository.observeRecoveryCodePrompt().first())
    }

    @Test
    fun `GIVEN an anonymous user WHEN connecting Apple in the browser THEN links and remembers the pending link`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)

            // When
            val result = repository.signInWithBrowser(AccountProvider.APPLE)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(AccountProvider.APPLE), remote.browserLinks)
            assertTrue(remote.browserSignIns.isEmpty())
            assertEquals(AccountProvider.APPLE.code, local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN no session WHEN connecting Google in the browser THEN signs in without a pending link`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.signInWithBrowser(AccountProvider.GOOGLE)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(AccountProvider.GOOGLE), remote.browserSignIns)
        assertTrue(remote.browserLinks.isEmpty())
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN a Google session WHEN connecting Apple in the browser THEN signs in without a pending link`() = runTest {
        // Given
        prepareScenario(currentUser = googleUser)

        // When
        val result = repository.signInWithBrowser(AccountProvider.APPLE)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
        assertTrue(remote.browserLinks.isEmpty())
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN no session and a stale pending link WHEN connecting in the browser THEN clears the pending link`() =
        runTest {
            // Given
            prepareScenario(currentUser = null, pendingLinkProvider = AccountProvider.GOOGLE)

            // When
            repository.signInWithBrowser(AccountProvider.APPLE)

            // Then
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN a URL that is not a callback arrives THEN keeps the pending link`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser, pendingLinkProvider = AccountProvider.APPLE, callback = null)
        val url = "https://app.example/"

        // When
        val result = repository.handleBrowserCallback(url)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(url), remote.readCallbackUrls)
        assertTrue(remote.browserSignIns.isEmpty())
        assertEquals(AccountProvider.APPLE.code, local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN a pending Apple link WHEN the identity belongs to another user THEN signs in to it with Apple`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.APPLE,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            )
            val url = "borarachar://auth-callback?error_code=identity_already_exists"

            // When
            val result = repository.handleBrowserCallback(url)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(url), remote.readCallbackUrls)
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
            assertTrue(remote.browserLinks.isEmpty())
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending Google link WHEN the identity belongs to another user THEN signs in to it with Google`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            )
            val url = "https://app.example/#error_code=identity_already_exists"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), remote.browserSignIns)
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN the link succeeded THEN clears the pending link without signing in again`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.APPLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            val url = "borarachar://auth-callback?code=abc"

            // When
            val result = repository.handleBrowserCallback(url)

            // Then
            assertTrue(result.isSuccess)
            assertTrue(remote.browserSignIns.isEmpty())
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN the link failed THEN clears the pending link without signing in`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            pendingLinkProvider = AccountProvider.GOOGLE,
            callback = BrowserCallback.FAILED,
        )
        val url = "https://app.example/#error=access_denied"

        // When
        val result = repository.handleBrowserCallback(url)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.browserSignIns.isEmpty())
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN no pending link WHEN the identity belongs to another user THEN does not start the sign in`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser, callback = BrowserCallback.IDENTITY_ALREADY_LINKED)
        val url = "https://app.example/#error_code=identity_already_exists"

        // When
        val result = repository.handleBrowserCallback(url)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.browserSignIns.isEmpty())
        assertTrue(remote.browserLinks.isEmpty())
    }

    @Test
    fun `GIVEN no pending link WHEN a sign in callback arrives THEN does nothing`() = runTest {
        // Given
        prepareScenario(currentUser = googleUser, callback = BrowserCallback.SIGNED_IN)
        val url = "borarachar://auth-callback?code=abc"

        // When
        val result = repository.handleBrowserCallback(url)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.browserSignIns.isEmpty())
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN a pending link WHEN the second browser trip fails to start THEN fails and keeps the pending link`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.APPLE,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
                browserSignInError = IllegalStateException("browser unavailable"),
            )
            val url = "borarachar://auth-callback?error_code=identity_already_exists"

            // When
            val result = repository.handleBrowserCallback(url)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(AccountProvider.APPLE.code, local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN an anonymous user WHEN the browser link fails to start THEN fails and leaves no pending link`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser, linkError = IllegalStateException("offline"))

            // When
            val result = repository.signInWithBrowser(AccountProvider.APPLE)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN the session signs out THEN clears the pending link`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser, pendingLinkProvider = AccountProvider.APPLE)
        remote.sessionUpdates = flowOf(null)

        // When
        repository.mirrorSession()

        // Then
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN no session and the Apple name WHEN signing in with Apple THEN keeps the name on the new user`() =
        runTest {
            // Given
            prepareScenario(currentUser = null, idTokenUser = appleUserWithoutName)

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
            assertEquals(listOf(appleName), remote.savedDisplayNames)
            assertEquals(appleName, repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN an anonymous user and the Apple name WHEN connecting Apple THEN keeps the name on the same user`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.LINKED, result.getOrNull())
            assertEquals(listOf(appleName), remote.savedDisplayNames)
            assertEquals(
                AccountStatus.Connected("anonymous", AccountProvider.APPLE),
                repository.observeStatus().first(),
            )
            assertEquals(appleName, repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN an Apple identity of a user without a name WHEN connecting it THEN keeps the name on that user`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IdentityAlreadyLinkedException(),
                idTokenUser = appleUserWithoutName,
            )

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertEquals(listOf(appleName), remote.savedDisplayNames)
            assertEquals(
                AccountStatus.Connected("apple-user", AccountProvider.APPLE),
                repository.observeStatus().first(),
            )
            assertEquals(appleName, repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN an Apple identity of a user with a name WHEN connecting it THEN keeps the name that user has`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IdentityAlreadyLinkedException(),
                idTokenUser = appleUserWithName,
            )

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertEquals("Pierre", repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN a Google session WHEN switching to an Apple user without a name THEN keeps the Apple name on it`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUser, idTokenUser = appleUserWithoutName)

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertEquals(listOf(appleName), remote.savedDisplayNames)
            assertEquals(
                AccountStatus.Connected("apple-user", AccountProvider.APPLE),
                repository.observeStatus().first(),
            )
        }

    @Test
    fun `GIVEN a Google session WHEN switching to an Apple user with a name THEN keeps the name that user has`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUser, idTokenUser = appleUserWithName)

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
        }

    @Test
    fun `GIVEN no session and a Google user that has a name WHEN signing in with a name THEN keeps the Google name`() =
        runTest {
            // Given
            prepareScenario(currentUser = null, idTokenUser = googleUser)

            // When
            val result = repository.signInWithIdToken(
                AccountProvider.GOOGLE,
                idToken = "token",
                rawNonce = null,
                fullName = appleName,
            )

            // Then
            assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertEquals("Pierre", repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN a later Apple sign in without a name WHEN signing in THEN saves no name`() = runTest {
        // Given
        prepareScenario(currentUser = null, idTokenUser = appleUserWithoutName)

        // When
        val result = signInWithApple(fullName = null)

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertTrue(remote.savedDisplayNames.isEmpty())
    }

    @Test
    fun `GIVEN an anonymous user connecting Google without a name WHEN linking THEN saves no name`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.GOOGLE,
            idToken = "token",
            rawNonce = "nonce",
            fullName = null,
        )

        // Then
        assertEquals(ConnectResult.LINKED, result.getOrNull())
        assertTrue(remote.savedDisplayNames.isEmpty())
    }

    @Test
    fun `GIVEN saving the name fails WHEN an anonymous user connects Apple THEN still links and mirrors the account`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser, saveDisplayNameError = IllegalStateException("offline"))

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.LINKED, result.getOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertEquals(
                AccountStatus.Connected("anonymous", AccountProvider.APPLE),
                repository.observeStatus().first(),
            )
        }

    @Test
    fun `GIVEN saving the name fails WHEN switching to an Apple user THEN still switches and mirrors the account`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IdentityAlreadyLinkedException(),
                idTokenUser = appleUserWithoutName,
                saveDisplayNameError = IllegalStateException("offline"),
            )

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertEquals(
                AccountStatus.Connected("apple-user", AccountProvider.APPLE),
                repository.observeStatus().first(),
            )
        }

    @Test
    fun `GIVEN linking Apple fails for another reason WHEN connecting it with the name THEN fails and saves no name`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser, linkError = IllegalStateException("offline"))

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertTrue(remote.signedInTokens.isEmpty())
            assertEquals(AccountStatus.LocalOnly, repository.observeStatus().first())
        }

    private val anonymousUser = SessionUser("anonymous", socialProvider = null, displayName = null, photoUrl = null)

    private val googleUser =
        SessionUser("user-1", socialProvider = AccountProvider.GOOGLE, displayName = "Pierre", photoUrl = null)

    private val appleName = "Pierre Vieira"

    private val appleUserWithoutName =
        SessionUser("apple-user", socialProvider = AccountProvider.APPLE, displayName = null, photoUrl = null)

    private val appleUserWithName =
        SessionUser("apple-user", socialProvider = AccountProvider.APPLE, displayName = "Pierre", photoUrl = null)

    private suspend fun signInWithApple(fullName: String?): Result<ConnectResult> = repository.signInWithIdToken(
        AccountProvider.APPLE,
        idToken = "token",
        rawNonce = "nonce",
        fullName = fullName,
    )

    private fun prepareScenario(
        currentUser: SessionUser?,
        linkError: Throwable? = null,
        issueError: Throwable? = null,
        pendingLinkProvider: AccountProvider? = null,
        callback: BrowserCallback? = null,
        browserSignInError: Throwable? = null,
        idTokenUser: SessionUser? = null,
        saveDisplayNameError: Throwable? = null,
    ) {
        local = FakeAccountLocalDataSource(pendingLinkProvider = pendingLinkProvider?.code)
        remote = FakeAccountRemoteDataSource(
            currentUser = currentUser,
            linkError = linkError,
            issueError = issueError,
            callback = callback,
            browserSignInError = browserSignInError,
            idTokenUser = idTokenUser,
        ).also { it.saveDisplayNameError = saveDisplayNameError }
        repository = AccountRepositoryImpl(local = local, remote = remote)
    }
}

private class FakeAccountLocalDataSource(
    pendingLinkProvider: String?,
) : AccountLocalDataSource {
    private val row = MutableStateFlow(
        AccountEntity(
            id = "me",
            userId = null,
            provider = null,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE.code,
            pendingLinkProvider = pendingLinkProvider,
            browserSignInProvider = null,
        ),
    )

    override fun observe(): Flow<AccountEntity> = row

    override suspend fun get(): AccountEntity = row.value

    override suspend fun update(transform: (AccountEntity) -> AccountEntity) {
        row.value = transform(row.value)
    }
}

private class FakeAccountRemoteDataSource(
    private var currentUser: SessionUser?,
    private val linkError: Throwable?,
    private val issueError: Throwable?,
    private val callback: BrowserCallback?,
    private val browserSignInError: Throwable?,
    // Why: the user the ID token belongs to, whom a sign in switches the session to; null keeps the session as is.
    private val idTokenUser: SessionUser?,
) : AccountRemoteDataSource {
    val sessionUsers = MutableStateFlow<SessionUser?>(null)
    val linkedTokens = mutableListOf<String>()
    val signedInTokens = mutableListOf<String>()
    val exchangedCodes = mutableListOf<String>()
    val browserLinks = mutableListOf<AccountProvider>()
    val browserSignIns = mutableListOf<AccountProvider>()
    val readCallbackUrls = mutableListOf<String>()
    val savedDisplayNames = mutableListOf<String>()
    var saveDisplayNameError: Throwable? = null
    var anonymousSignIns = 0
    var sessionUpdates: Flow<SessionUser?> = emptyFlow()

    override fun observeSessionUser(): Flow<SessionUser?> = sessionUpdates

    override suspend fun getCurrentUserOrNull(): SessionUser? = currentUser ?: sessionUsers.value

    override suspend fun signInAnonymously(): SessionUser {
        anonymousSignIns++
        return SessionUser("anonymous", null, null, null).also { currentUser = it }
    }

    override suspend fun linkIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ) {
        linkError?.let { throw it }
        linkedTokens += idToken
        currentUser = currentUser?.copy(socialProvider = provider)
    }

    override suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ) {
        signedInTokens += idToken
        idTokenUser?.let { currentUser = it }
    }

    override suspend fun saveDisplayName(name: String) {
        saveDisplayNameError?.let { throw it }
        savedDisplayNames += name
        currentUser = (currentUser ?: sessionUsers.value)?.copy(displayName = name)
    }

    override suspend fun linkWithBrowser(provider: AccountProvider) {
        linkError?.let { throw it }
        browserLinks += provider
    }

    override suspend fun signInWithBrowser(provider: AccountProvider) {
        browserSignInError?.let { throw it }
        browserSignIns += provider
    }

    override fun readBrowserCallback(url: String): BrowserCallback? {
        readCallbackUrls += url
        return callback
    }

    override suspend fun signInWithPassword(credentials: RecoveryCredentials) {
        currentUser = SessionUser("restored", null, null, null)
    }

    override suspend fun fetchRecoveryCredentials(code: String): RecoveryCredentials {
        exchangedCodes += code
        return RecoveryCredentials(email = "restored@recovery.borarachar.com", password = "secret")
    }

    override suspend fun createRecoveryCode(): String {
        issueError?.let { throw it }
        return "K7QM4XPA9TRE"
    }
}
