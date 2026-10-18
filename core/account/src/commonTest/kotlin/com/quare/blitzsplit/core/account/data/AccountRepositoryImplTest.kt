package com.quare.blitzsplit.core.account.data

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        val result = repository.signInWithIdToken(AccountProvider.GOOGLE, idToken = "token", rawNonce = "nonce")

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
        val result = repository.signInWithIdToken(AccountProvider.APPLE, idToken = "token", rawNonce = null)

        // Then
        assertEquals(ConnectResult.SWITCHED, result.getOrNull())
        assertEquals(listOf("token"), remote.signedInTokens)
    }

    @Test
    fun `GIVEN no session WHEN connecting THEN signs in with the token`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.signInWithIdToken(AccountProvider.GOOGLE, idToken = "token", rawNonce = null)

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

    private val anonymousUser = SessionUser("anonymous", socialProvider = null, displayName = null, photoUrl = null)

    private fun prepareScenario(
        currentUser: SessionUser?,
        linkError: Throwable? = null,
        issueError: Throwable? = null,
    ) {
        local = FakeAccountLocalDataSource()
        remote = FakeAccountRemoteDataSource(currentUser, linkError, issueError)
        repository = AccountRepositoryImpl(local = local, remote = remote)
    }
}

private class FakeAccountLocalDataSource : AccountLocalDataSource {
    private val row = MutableStateFlow(
        AccountEntity(
            id = "me",
            userId = null,
            provider = null,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE.code,
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
) : AccountRemoteDataSource {
    val sessionUsers = MutableStateFlow<SessionUser?>(null)
    val linkedTokens = mutableListOf<String>()
    val signedInTokens = mutableListOf<String>()
    val exchangedCodes = mutableListOf<String>()
    var anonymousSignIns = 0

    override fun observeSessionUser(): Flow<SessionUser?> = emptyFlow()

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
    }

    override suspend fun signInWithIdToken(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ) {
        signedInTokens += idToken
    }

    override suspend fun linkWithBrowser(provider: AccountProvider) {
        error("unused")
    }

    override suspend fun signInWithBrowser(provider: AccountProvider) {
        error("unused")
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
