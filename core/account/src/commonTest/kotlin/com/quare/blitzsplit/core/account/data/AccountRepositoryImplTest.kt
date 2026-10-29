package com.quare.blitzsplit.core.account.data

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.datasource.BrowserCallback
import com.quare.blitzsplit.core.account.data.repository.AccountRepositoryImpl
import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.account.domain.model.AppleToken
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryImplTest {
    private lateinit var local: FakeAccountLocalDataSource
    private lateinit var remote: FakeAccountRemoteDataSource
    private lateinit var repository: AccountRepositoryImpl
    private var abandonedSignIns = 0
    private val abandoned = mutableListOf<AbandonedBrowserSignIn>()

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
            authorizationCode = null,
        )

        // Then
        assertEquals(ConnectResult.LINKED, result.getOrNull())
        assertEquals(listOf("token"), remote.linkedTokens)
        assertTrue(remote.signedInTokens.isEmpty())
    }

    @Test
    fun `GIVEN the identity belongs to another user WHEN connecting THEN signs in to that user`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            linkError = IdentityAlreadyLinkedException(),
        )

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.APPLE,
            idToken = "token",
            rawNonce = null,
            fullName = null,
            authorizationCode = null,
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
            authorizationCode = null,
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
            remote.sessionUsers.value = SessionUser("user-1", AccountProvider.GOOGLE, "Pierre", null, null)

            // When
            repository.ensureRegistered()

            // Then
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), repository.observeStatus().first())
            assertEquals(RecoveryCodePrompt.SAVED, repository.observeAccount().first().recoveryCodePrompt)
        }

    @Test
    fun `GIVEN an anonymous user WHEN issuing a recovery code THEN stores it and makes the prompt due`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = repository.createRecoveryCode()

        // Then
        assertEquals("K7QM4XPA9TRE", result.getOrNull())
        assertEquals("K7QM4XPA9TRE", repository.observeAccount().first().recoveryCode)
        assertEquals(RecoveryCodePrompt.DUE, repository.observeAccount().first().recoveryCodePrompt)
    }

    @Test
    fun `GIVEN a recovery code WHEN observing the account THEN emits the status code and prompt together`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        repository.ensureRegistered()
        repository.createRecoveryCode()

        // When
        val account = repository.observeAccount().first()

        // Then
        assertEquals(
            AccountSnapshot(
                status = AccountStatus.Anonymous("anonymous"),
                recoveryCode = "K7QM4XPA9TRE",
                recoveryCodePrompt = RecoveryCodePrompt.DUE,
                email = null,
                signOutWhenSynced = false,
            ),
            account,
        )
    }

    @Test
    fun `GIVEN a social account WHEN issuing a recovery code THEN settles the prompt`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            issueError = SocialAccountHasNoRecoveryCodeException(),
        )

        // When
        val result = repository.createRecoveryCode()

        // Then
        assertIs<SocialAccountHasNoRecoveryCodeException>(result.exceptionOrNull())
        assertEquals(RecoveryCodePrompt.SAVED, repository.observeAccount().first().recoveryCodePrompt)
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
        assertEquals(RecoveryCodePrompt.SAVED, repository.observeAccount().first().recoveryCodePrompt)
    }

    @Test
    fun `GIVEN an anonymous user WHEN connecting Apple in the browser THEN links and remembers the pending link`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)

            // When
            val result = repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

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
        val result = repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

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
        val result = repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

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
            prepareScenario(
                currentUser = null,
                pendingLinkProvider = AccountProvider.GOOGLE,
            )

            // When
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // Then
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN a URL that is not a callback arrives THEN keeps the pending link`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            pendingLinkProvider = AccountProvider.APPLE,
            callback = null,
        )
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
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
        )
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
        prepareScenario(
            currentUser = googleUser,
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
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IllegalStateException("offline"),
            )

            // When
            val result = repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertNull(local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN a pending link WHEN the session signs out THEN clears the pending link`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            pendingLinkProvider = AccountProvider.APPLE,
        )
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
            prepareScenario(
                currentUser = null,
                idTokenUser = appleUserWithoutName,
            )

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
            prepareScenario(
                currentUser = googleUser,
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
        }

    @Test
    fun `GIVEN a Google session WHEN switching to an Apple user with a name THEN keeps the name that user has`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = googleUser,
                idTokenUser = appleUserWithName,
            )

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
            prepareScenario(
                currentUser = null,
                idTokenUser = googleUser,
            )

            // When
            val result = repository.signInWithIdToken(
                AccountProvider.GOOGLE,
                idToken = "token",
                rawNonce = null,
                fullName = appleName,
                authorizationCode = null,
            )

            // Then
            assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertEquals("Pierre", repository.getCurrentUserOrNull()?.displayName)
        }

    @Test
    fun `GIVEN a later Apple sign in without a name WHEN signing in THEN saves no name`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            idTokenUser = appleUserWithoutName,
        )

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
            authorizationCode = null,
        )

        // Then
        assertEquals(ConnectResult.LINKED, result.getOrNull())
        assertTrue(remote.savedDisplayNames.isEmpty())
    }

    @Test
    fun `GIVEN saving the name fails WHEN an anonymous user connects Apple THEN still links and mirrors the account`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                saveDisplayNameError = IllegalStateException("offline"),
            )

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
    fun `GIVEN a Google sign in in the browser WHEN the app resumes without a callback THEN reports it abandoned`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN a Google session signing in to Apple in the browser WHEN the app resumes THEN reports it abandoned`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUser)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN an anonymous user linking Apple in the browser WHEN the app resumes THEN reports it abandoned`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(listOf(AccountProvider.APPLE), remote.browserLinks)
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN an abandoned sign in already reported WHEN the app resumes again THEN reports nothing more`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(1, abandonedSignIns)
    }

    @Test
    fun `GIVEN no browser trip WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        observeAbandonedSignIns()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN the browser sign in failed to start WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInError = IllegalStateException("browser unavailable"),
        )
        observeAbandonedSignIns()
        val result = repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN the browser link failed to start WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            linkError = IllegalStateException("offline"),
        )
        observeAbandonedSignIns()
        val result = repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN a browser sign in whose callback signed in WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.SIGNED_IN,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleBrowserCallback(signInCallbackUrl)
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN a browser link whose callback linked WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.SIGNED_IN,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        repository.handleBrowserCallback(signInCallbackUrl)
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN a sign in callback still completing WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.SIGNED_IN,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        local.reads = CompletableDeferred()
        val completing = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.handleBrowserCallback(signInCallbackUrl)
        }

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertTrue(completing.isActive)
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN a browser sign in WHEN the callback says it failed THEN reports it abandoned right away`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.FAILED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        val url = "borarachar://auth-callback?error=access_denied"

        // When
        val result = repository.handleBrowserCallback(url)
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, abandonedSignIns)
    }

    @Test
    fun `GIVEN a browser link WHEN the callback says it failed THEN reports it abandoned right away`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.FAILED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        val url = "borarachar://auth-callback?error=access_denied"

        // When
        val result = repository.handleBrowserCallback(url)
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, abandonedSignIns)
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN a failed callback already reported WHEN the app resumes THEN reports nothing more`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.FAILED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleBrowserCallback("borarachar://auth-callback?error=access_denied")
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(1, abandonedSignIns)
    }

    @Test
    fun `GIVEN a link whose identity belongs to another user WHEN the app resumes from that sign in THEN reports it`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
            repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
            runCurrent()

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
            assertNull(local.get().pendingLinkProvider)
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN the sign in to the identity owner signed in WHEN the app resumes THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
        remote.callback = BrowserCallback.SIGNED_IN
        repository.handleBrowserCallback(signInCallbackUrl)
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(0, abandonedSignIns)
    }

    @Test
    fun `GIVEN a link whose owner sign in fails to start WHEN the app resumes THEN reported it abandoned once`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
                browserSignInError = IllegalStateException("browser unavailable"),
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
            val result =
                repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN a browser sign in and a URL that is not a callback WHEN the app resumes THEN reports it abandoned`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                callback = null,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleBrowserCallback("https://app.example/")
            runCurrent()

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(1, abandonedSignIns)
        }

    @Test
    fun `GIVEN a sign in abandoned while nobody observed WHEN a screen starts observing THEN receives nothing`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleAppPaused()
            repository.handleAppResumed()

            // When
            observeAbandonedSignIns()
            runCurrent()

            // Then
            assertEquals(0, abandonedSignIns)
        }

    @Test
    fun `GIVEN a browser sign in just opened WHEN the app resumes without having left THEN reports nothing`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

            // When
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN a resume with the browser still open WHEN the app leaves and comes back THEN it was cancelled`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleAppResumed()
            runCurrent()

            // When
            repository.handleAppPaused()
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
        }

    @Test
    fun `GIVEN a pause before the browser opened WHEN the app resumes without leaving THEN reports nothing`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            observeAbandonedSignIns()
            repository.handleAppPaused()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

            // When
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN a browser sign in the app left for WHEN the app comes back without a callback THEN it was cancelled`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleAppPaused()

            // When
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
        }

    @Test
    fun `GIVEN a link whose identity has an owner WHEN the return resumes the app THEN keeps the owner sign in open`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
            repository.handleAppPaused()
            repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")

            // When
            repository.handleAppResumed()
            runCurrent()

            // Then
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN the owner sign in kept open WHEN the app leaves and comes back THEN it was cancelled`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        repository.handleAppPaused()
        repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
        repository.handleAppResumed()
        runCurrent()

        // When
        repository.handleAppPaused()
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
    }

    @Test
    fun `GIVEN a browser sign in WHEN the callback says the person cancelled THEN cancels at once`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.CANCELLED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleAppPaused()

        // When
        val result = repository.handleBrowserCallback("borarachar://auth-callback?error=access_denied")
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
    }

    @Test
    fun `GIVEN a browser link WHEN the callback says the person cancelled THEN cancels and clears the link`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                callback = BrowserCallback.CANCELLED,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
            repository.handleAppPaused()

            // When
            val result = repository.handleBrowserCallback("borarachar://auth-callback?error=user_cancelled_authorize")
            runCurrent()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
            assertNull(local.get().pendingLinkProvider)
            assertTrue(remote.browserSignIns.isEmpty())
        }

    @Test
    fun `GIVEN a cancel callback already reported WHEN the app comes back THEN reports nothing more`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.CANCELLED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleAppPaused()
        repository.handleBrowserCallback("borarachar://auth-callback?error=access_denied")
        runCurrent()

        // When
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertEquals(listOf(AbandonedBrowserSignIn.CANCELLED), abandoned)
    }

    @Test
    fun `GIVEN a browser sign in WHEN the callback says it failed THEN reports a failure`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.FAILED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
        repository.handleAppPaused()

        // When
        repository.handleBrowserCallback("borarachar://auth-callback?error=server_error")
        runCurrent()

        // Then
        assertEquals(listOf(AbandonedBrowserSignIn.FAILED), abandoned)
    }

    @Test
    fun `GIVEN a Google session signing in to Apple WHEN the identity has an owner THEN reports a failure`() = runTest {
        // Given
        prepareScenario(
            currentUser = googleUser,
            callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        repository.handleAppPaused()

        // When
        val result =
            repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
        assertEquals(listOf(AbandonedBrowserSignIn.FAILED), abandoned)
    }

    @Test
    fun `GIVEN a link whose owner sign in fails to start WHEN the callback arrives THEN reports a failure`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            browserSignInError = IllegalStateException("browser unavailable"),
        )
        observeAbandonedSignIns()
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        repository.handleAppPaused()

        // When
        val result =
            repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
        runCurrent()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(AbandonedBrowserSignIn.FAILED), abandoned)
    }

    @Test
    fun `GIVEN an open browser trip and an unreadable account WHEN an error callback arrives THEN reports a failure`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                callback = BrowserCallback.FAILED,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleAppPaused()
            local.getError = IllegalStateException("database closed")

            // When
            val result = repository.handleBrowserCallback("borarachar://auth-callback?error=server_error")
            runCurrent()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(listOf(AbandonedBrowserSignIn.FAILED), abandoned)
        }

    @Test
    fun `GIVEN an open browser trip and an unreadable account WHEN a sign in callback arrives THEN reports nothing`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                callback = BrowserCallback.SIGNED_IN,
            )
            observeAbandonedSignIns()
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)
            repository.handleAppPaused()
            local.getError = IllegalStateException("database closed")

            // When
            val result = repository.handleBrowserCallback(signInCallbackUrl)
            runCurrent()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN a failed callback after a process death WHEN the app comes back THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            callback = BrowserCallback.FAILED,
        )
        observeAbandonedSignIns()
        repository.handleBrowserCallback("borarachar://auth-callback?error=server_error")

        // When
        repository.handleAppResumed()
        runCurrent()

        // Then
        assertTrue(abandoned.isEmpty())
    }

    @Test
    fun `GIVEN no browser trip and an unreadable account WHEN a callback arrives THEN fails and reports nothing`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                callback = BrowserCallback.SIGNED_IN,
            )
            observeAbandonedSignIns()
            local.getError = IllegalStateException("database closed")

            // When
            val result = repository.handleBrowserCallback(signInCallbackUrl)
            runCurrent()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN a pending link from before a process death WHEN the owner sign in fails THEN reports nothing`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.APPLE,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
                browserSignInError = IllegalStateException("browser unavailable"),
            )
            observeAbandonedSignIns()

            // When
            val result =
                repository.handleBrowserCallback("borarachar://auth-callback?error_code=identity_already_exists")
            runCurrent()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(abandoned.isEmpty())
        }

    @Test
    fun `GIVEN linking Apple fails for another reason WHEN connecting it with the name THEN fails and saves no name`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IllegalStateException("offline"),
            )

            // When
            val result = signInWithApple(fullName = appleName)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.savedDisplayNames.isEmpty())
            assertTrue(remote.signedInTokens.isEmpty())
            assertEquals(AccountStatus.LocalOnly, repository.observeStatus().first())
        }

    @Test
    fun `GIVEN no session WHEN signing in with Google in the browser THEN remembers Google as the browser sign in`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)

            // When
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

            // Then
            assertEquals(AccountProvider.GOOGLE.code, local.get().browserSignInProvider)
        }

    @Test
    fun `GIVEN a Google session WHEN signing in with Apple in the browser THEN remembers the Apple browser sign in`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = googleUser,
                browserSignInProvider = AccountProvider.GOOGLE,
            )

            // When
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // Then
            assertEquals(AccountProvider.APPLE.code, local.get().browserSignInProvider)
        }

    @Test
    fun `GIVEN an anonymous user WHEN linking Google in the browser THEN remembers Google as the browser sign in`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)

            // When
            repository.signInWithBrowser(AccountProvider.GOOGLE, isFromProfile = false)

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), remote.browserLinks)
            assertEquals(AccountProvider.GOOGLE.code, local.get().browserSignInProvider)
            assertEquals(AccountProvider.GOOGLE.code, local.get().pendingLinkProvider)
        }

    @Test
    fun `GIVEN an app start without a callback WHEN observing the returning browser sign in THEN there is none`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
            )

            // When
            val returning = repository.observeReturningBrowserSignIn().first()

            // Then
            assertNull(returning)
        }

    @Test
    fun `GIVEN a Google browser sign in WHEN its callback arrives THEN shows Google as the returning sign in`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            val url = "https://app.example/#access_token=abc"

            // When
            val result = repository.handleBrowserCallback(url)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(AccountProvider.GOOGLE, repository.observeReturningBrowserSignIn().first())
        }

    @Test
    fun `GIVEN an anonymous user linking Apple in the browser WHEN its callback arrives THEN shows Apple`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            callback = BrowserCallback.SIGNED_IN,
        )
        repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)
        val url = "https://app.example/#access_token=abc"

        // When
        repository.handleBrowserCallback(url)

        // Then
        assertEquals(AccountProvider.APPLE, repository.observeReturningBrowserSignIn().first())
        assertNull(local.get().pendingLinkProvider)
    }

    @Test
    fun `GIVEN a returning Google sign in WHEN the Google session is mirrored THEN the returning sign in ends`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            startMirroring()
            repository.handleBrowserCallback("https://app.example/#access_token=abc")

            // When
            sessions.emit(googleUser)
            runCurrent()

            // Then
            assertNull(repository.observeReturningBrowserSignIn().first())
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), repository.observeStatus().first())
        }

    @Test
    fun `GIVEN a returning Apple sign in WHEN an anonymous session is mirrored THEN the returning sign in ends`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                browserSignInProvider = AccountProvider.APPLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            startMirroring()
            repository.handleBrowserCallback("https://app.example/#access_token=abc")

            // When
            sessions.emit(anonymousUser)
            runCurrent()

            // Then
            assertNull(repository.observeReturningBrowserSignIn().first())
        }

    @Test
    fun `GIVEN a returning Google sign in WHEN no session is mirrored THEN the returning sign in ends`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInProvider = AccountProvider.GOOGLE,
            callback = BrowserCallback.SIGNED_IN,
        )
        startMirroring()
        repository.handleBrowserCallback("https://app.example/#access_token=abc")

        // When
        sessions.emit(null)
        runCurrent()

        // Then
        assertNull(repository.observeReturningBrowserSignIn().first())
    }

    @Test
    fun `GIVEN the session mirrored before the saved provider is read WHEN the callback arrives THEN shows nothing`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            startMirroring()
            local.beforeNextGet = {
                sessions.emit(googleUser)
                yield()
            }

            // When
            repository.handleBrowserCallback("https://app.example/#access_token=abc")

            // Then
            assertNull(repository.observeReturningBrowserSignIn().first())
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), repository.observeStatus().first())
        }

    @Test
    fun `GIVEN a Google browser sign in WHEN a URL that is not a callback arrives THEN shows no returning sign in`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = null,
            )
            val url = "https://app.example/"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertNull(repository.observeReturningBrowserSignIn().first())
        }

    @Test
    fun `GIVEN a Google browser sign in WHEN its callback failed THEN shows no returning sign in`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInProvider = AccountProvider.GOOGLE,
            callback = BrowserCallback.FAILED,
        )
        val url = "https://app.example/#error=access_denied"

        // When
        repository.handleBrowserCallback(url)

        // Then
        assertNull(repository.observeReturningBrowserSignIn().first())
    }

    @Test
    fun `GIVEN a pending Apple link WHEN the identity belongs to another user THEN shows no returning sign in`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                pendingLinkProvider = AccountProvider.APPLE,
                browserSignInProvider = AccountProvider.APPLE,
                callback = BrowserCallback.IDENTITY_ALREADY_LINKED,
            )
            val url = "https://app.example/#error_code=identity_already_exists"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertNull(repository.observeReturningBrowserSignIn().first())
            assertEquals(listOf(AccountProvider.APPLE), remote.browserSignIns)
        }

    @Test
    fun `GIVEN an old install with no saved browser sign in WHEN a sign in callback arrives THEN shows nothing`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                callback = BrowserCallback.SIGNED_IN,
            )
            val url = "https://app.example/#access_token=abc"

            // When
            val result = repository.handleBrowserCallback(url)

            // Then
            assertTrue(result.isSuccess)
            assertNull(repository.observeReturningBrowserSignIn().first())
        }

    @Test
    fun `GIVEN an earlier returning sign in already mirrored WHEN a second callback arrives THEN shows it again`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = googleUser,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            startMirroring()
            repository.handleBrowserCallback("borarachar://auth-callback?code=first")
            sessions.emit(googleUser)
            runCurrent()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // When
            repository.handleBrowserCallback("borarachar://auth-callback?code=second")

            // Then
            assertEquals(AccountProvider.APPLE, repository.observeReturningBrowserSignIn().first())
        }

    @Test
    fun `GIVEN a Google session with an email WHEN the session is mirrored THEN the account keeps that email`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()

            // When
            sessions.emit(googleUserWithEmail)
            runCurrent()

            // Then
            assertEquals(pierreEmail, repository.observeAccount().first().email)
        }

    @Test
    fun `GIVEN an Apple session with an email WHEN the session is mirrored THEN the account keeps that email`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()

            // When
            sessions.emit(appleUserWithEmail)
            runCurrent()

            // Then
            assertEquals(appleRelayEmail, repository.observeAccount().first().email)
        }

    @Test
    fun `GIVEN a Google session without an email WHEN the session is mirrored THEN the account has no email`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()

            // When
            sessions.emit(googleUser)
            runCurrent()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), account.status)
            assertNull(account.email)
        }

    @Test
    fun `GIVEN an anonymous session with a recovery email WHEN it is mirrored THEN the account has no email`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()

            // When
            sessions.emit(restoredUserWithRecoveryEmail)
            runCurrent()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(AccountStatus.Anonymous("restored"), account.status)
            assertNull(account.email)
        }

    @Test
    fun `GIVEN a connected account with an email WHEN the session ends THEN the account has no email`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        startMirroring()
        sessions.emit(googleUserWithEmail)
        runCurrent()

        // When
        sessions.emit(null)
        runCurrent()

        // Then
        val account = repository.observeAccount().first()
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(account.email)
    }

    @Test
    fun `GIVEN a connected account with an email WHEN an anonymous session is mirrored THEN drops the email`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()
            sessions.emit(googleUserWithEmail)
            runCurrent()

            // When
            sessions.emit(anonymousUser)
            runCurrent()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(AccountStatus.Anonymous("anonymous"), account.status)
            assertNull(account.email)
        }

    @Test
    fun `GIVEN no session WHEN signing in with Google THEN the account keeps the Google email`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            idTokenUser = googleUserWithEmail,
        )

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.GOOGLE,
            idToken = "token",
            rawNonce = null,
            fullName = null,
            authorizationCode = null,
        )

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertEquals(pierreEmail, repository.observeAccount().first().email)
    }

    @Test
    fun `GIVEN a Google session with an email WHEN ensuring registration THEN the account keeps that email`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUserWithEmail)

            // When
            repository.ensureRegistered()

            // Then
            assertEquals(pierreEmail, repository.observeAccount().first().email)
        }

    @Test
    fun `GIVEN no sign out scheduled WHEN scheduling the sign out once synced THEN the account reports it`() = runTest {
        // Given
        prepareScenario(currentUser = googleUserWithEmail)
        val isScheduled = true

        // When
        repository.updateSignOutWhenSynced(isScheduled)

        // Then
        assertTrue(repository.observeAccount().first().signOutWhenSynced)
    }

    @Test
    fun `GIVEN a sign out scheduled once synced WHEN turning it off THEN the account no longer reports it`() = runTest {
        // Given
        prepareScenario(currentUser = googleUserWithEmail)
        repository.updateSignOutWhenSynced(true)
        val isScheduled = false

        // When
        repository.updateSignOutWhenSynced(isScheduled)

        // Then
        assertFalse(repository.observeAccount().first().signOutWhenSynced)
    }

    @Test
    fun `GIVEN a Google account with a scheduled sign out WHEN signing out THEN ends the session as local only`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUserWithEmail)
            repository.ensureRegistered()
            repository.updateSignOutWhenSynced(true)

            // When
            repository.signOut()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(1, remote.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(account.email)
            assertNull(account.recoveryCode)
            assertFalse(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN an anonymous user with a recovery code WHEN signing out THEN forgets the code and goes local only`() =
        runTest {
            // Given
            prepareScenario(currentUser = anonymousUser)
            repository.ensureRegistered()
            repository.createRecoveryCode()

            // When
            repository.signOut()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(1, remote.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(account.recoveryCode)
            assertNull(account.email)
            assertFalse(account.signOutWhenSynced)
            assertNull(repository.getCurrentUserOrNull())
        }

    @Test
    fun `GIVEN ending the server session fails WHEN signing out THEN fails and keeps the account as it was`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUserWithEmail)
            repository.ensureRegistered()
            repository.updateSignOutWhenSynced(true)
            val before = repository.observeAccount().first()
            remote.signOutError = IllegalStateException("offline")

            // When
            val error = runCatching { repository.signOut() }.exceptionOrNull()

            // Then
            assertIs<IllegalStateException>(error)
            assertEquals(before, repository.observeAccount().first())
            assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), before.status)
            assertEquals(pierreEmail, before.email)
            assertTrue(before.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a Google sign in and its session WHEN the session ends on its own THEN drops the scheduled sign out`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()
            sessions.emit(googleUserWithEmail)
            runCurrent()
            repository.updateSignOutWhenSynced(true)

            // When
            sessions.emit(null)
            runCurrent()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertFalse(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a Google account with a scheduled sign out WHEN the session switches to another user THEN drops it`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startMirroring()
            sessions.emit(googleUserWithEmail)
            runCurrent()
            repository.updateSignOutWhenSynced(true)

            // When
            sessions.emit(appleUserWithEmail)
            runCurrent()

            // Then
            val account = repository.observeAccount().first()
            assertEquals(AccountStatus.Connected("apple-user", AccountProvider.APPLE), account.status)
            assertFalse(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a Google account with a scheduled sign out WHEN its session refreshes THEN keeps it`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        startMirroring()
        sessions.emit(googleUserWithEmail)
        runCurrent()
        repository.updateSignOutWhenSynced(true)

        // When
        sessions.emit(googleUserWithEmail.copy(displayName = "Pierre Vieira"))
        runCurrent()

        // Then
        assertTrue(repository.observeAccount().first().signOutWhenSynced)
    }

    @Test
    fun `GIVEN a sign in left from the profile WHEN its callback relaunches the app THEN returns to the profile`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                browserSignInFromProfile = true,
                callback = BrowserCallback.SIGNED_IN,
            )
            val url = "https://app.example/#access_token=abc"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertEquals(AccountProvider.GOOGLE, repository.observeBrowserSignInReturningToProfile().first())
        }

    @Test
    fun `GIVEN a sign in left from the groups WHEN its callback relaunches the app THEN stays where it lands`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = null,
                browserSignInProvider = AccountProvider.GOOGLE,
                callback = BrowserCallback.SIGNED_IN,
            )
            val url = "https://app.example/#access_token=abc"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertEquals(AccountProvider.GOOGLE, repository.observeReturningBrowserSignIn().first())
            assertNull(repository.observeBrowserSignInReturningToProfile().first())
        }

    @Test
    fun `GIVEN a profile link opened by this app WHEN its callback arrives THEN keeps the profile on screen`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                callback = BrowserCallback.SIGNED_IN,
            )
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = true)
            val url = "https://app.example/#access_token=abc"

            // When
            repository.handleBrowserCallback(url)

            // Then
            assertTrue(local.get().browserSignInFromProfile)
            assertEquals(AccountProvider.APPLE, repository.observeReturningBrowserSignIn().first())
            assertNull(repository.observeBrowserSignInReturningToProfile().first())
        }

    @Test
    fun `GIVEN no session WHEN signing in with Apple natively THEN keeps its authorization code`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = signInWithApple(authorizationCode = appleAuthorizationCode)

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertEquals(listOf<AppleToken>(AppleToken.AuthorizationCode(appleAuthorizationCode)), remote.keptAppleTokens)
    }

    @Test
    fun `GIVEN an anonymous user WHEN connecting Apple natively THEN keeps its authorization code`() = runTest {
        // Given
        prepareScenario(currentUser = anonymousUser)

        // When
        val result = signInWithApple(authorizationCode = appleAuthorizationCode)

        // Then
        assertEquals(ConnectResult.LINKED, result.getOrNull())
        assertEquals(listOf<AppleToken>(AppleToken.AuthorizationCode(appleAuthorizationCode)), remote.keptAppleTokens)
    }

    @Test
    fun `GIVEN a Google session WHEN switching to Apple natively THEN keeps its authorization code`() = runTest {
        // Given
        prepareScenario(currentUser = googleUser)

        // When
        val result = signInWithApple(authorizationCode = appleAuthorizationCode)

        // Then
        assertEquals(ConnectResult.SWITCHED, result.getOrNull())
        assertEquals(listOf<AppleToken>(AppleToken.AuthorizationCode(appleAuthorizationCode)), remote.keptAppleTokens)
    }

    @Test
    fun `GIVEN an Apple identity of another user WHEN connecting Apple natively THEN keeps its authorization code`() =
        runTest {
            // Given
            prepareScenario(
                currentUser = anonymousUser,
                linkError = IdentityAlreadyLinkedException(),
            )

            // When
            val result = signInWithApple(authorizationCode = appleAuthorizationCode)

            // Then
            assertEquals(ConnectResult.SWITCHED, result.getOrNull())
            assertEquals(
                listOf<AppleToken>(AppleToken.AuthorizationCode(appleAuthorizationCode)),
                remote.keptAppleTokens,
            )
        }

    @Test
    fun `GIVEN a Google sign in with a code WHEN signing in with Google THEN keeps no Apple token`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = repository.signInWithIdToken(
            AccountProvider.GOOGLE,
            idToken = "token",
            rawNonce = null,
            fullName = null,
            authorizationCode = appleAuthorizationCode,
        )

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertTrue(remote.keptAppleTokens.isEmpty())
    }

    @Test
    fun `GIVEN an Apple sign in without a code WHEN signing in with Apple THEN keeps no Apple token`() = runTest {
        // Given
        prepareScenario(currentUser = null)

        // When
        val result = signInWithApple(authorizationCode = null)

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertTrue(remote.keptAppleTokens.isEmpty())
    }

    @Test
    fun `GIVEN the server fails to keep the Apple token WHEN signing in with Apple THEN still signs in`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            idTokenUser = appleUserWithName,
        )
        remote.keepAppleTokenError = IllegalStateException("server error")

        // When
        val result = signInWithApple(authorizationCode = appleAuthorizationCode)

        // Then
        assertEquals(ConnectResult.SIGNED_IN, result.getOrNull())
        assertTrue(remote.keptAppleTokens.isEmpty())
        assertEquals(AccountStatus.Connected("apple-user", AccountProvider.APPLE), repository.observeStatus().first())
    }

    @Test
    fun `GIVEN linking Apple fails WHEN connecting Apple natively THEN fails and keeps no Apple token`() = runTest {
        // Given
        prepareScenario(
            currentUser = anonymousUser,
            linkError = IllegalStateException("offline"),
        )

        // When
        val result = signInWithApple(authorizationCode = appleAuthorizationCode)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(remote.keptAppleTokens.isEmpty())
    }

    @Test
    fun `GIVEN an Apple browser sign in WHEN its refresh token arrives THEN keeps it`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInProvider = AccountProvider.APPLE,
        )
        startKeepingAppleTokens()

        // When
        remote.appleRefreshTokens.emit("refresh-1")
        runCurrent()

        // Then
        assertEquals(listOf<AppleToken>(AppleToken.RefreshToken("refresh-1")), remote.keptAppleTokens)
    }

    @Test
    fun `GIVEN no browser sign in WHEN a refresh token arrives THEN keeps nothing`() = runTest {
        // Given
        prepareScenario(currentUser = null)
        startKeepingAppleTokens()

        // When
        remote.appleRefreshTokens.emit("refresh-1")
        runCurrent()

        // Then
        assertTrue(remote.keptAppleTokens.isEmpty())
    }

    @Test
    fun `GIVEN a Google browser sign in WHEN a refresh token arrives THEN keeps nothing`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInProvider = AccountProvider.GOOGLE,
        )
        startKeepingAppleTokens()

        // When
        remote.appleRefreshTokens.emit("refresh-1")
        runCurrent()

        // Then
        assertTrue(remote.keptAppleTokens.isEmpty())
    }

    @Test
    fun `GIVEN keeping one refresh token failed WHEN the next one arrives THEN keeps the next one`() = runTest {
        // Given
        prepareScenario(
            currentUser = null,
            browserSignInProvider = AccountProvider.APPLE,
        )
        startKeepingAppleTokens()
        remote.keepAppleTokenError = IllegalStateException("server error")
        remote.appleRefreshTokens.emit("refresh-1")
        runCurrent()
        remote.keepAppleTokenError = null

        // When
        remote.appleRefreshTokens.emit("refresh-2")
        runCurrent()

        // Then
        assertEquals(listOf<AppleToken>(AppleToken.RefreshToken("refresh-2")), remote.keptAppleTokens)
    }

    @Test
    fun `GIVEN a token that came before the Apple browser sign in WHEN one comes after it THEN keeps only the later`() =
        runTest {
            // Given
            prepareScenario(currentUser = null)
            startKeepingAppleTokens()
            remote.appleRefreshTokens.emit("refresh-early")
            runCurrent()
            repository.signInWithBrowser(AccountProvider.APPLE, isFromProfile = false)

            // When
            remote.appleRefreshTokens.emit("refresh-late")
            runCurrent()

            // Then
            assertEquals(listOf<AppleToken>(AppleToken.RefreshToken("refresh-late")), remote.keptAppleTokens)
        }

    @Test
    fun `GIVEN a Google account WHEN disconnecting Google THEN the same user becomes anonymous with a new code due`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUserWithEmail)
            repository.ensureRegistered()

            // When
            val result = repository.removeSocialIdentity()

            // Then
            val account = repository.observeAccount().first()
            assertTrue(result.isSuccess)
            assertEquals(1, remote.unlinkCount)
            assertEquals(AccountStatus.Anonymous("user-1"), account.status)
            assertEquals("P3XR7MQA2TKE", account.recoveryCode)
            assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
            assertNull(account.email)
        }

    @Test
    fun `GIVEN an Apple account WHEN disconnecting Apple THEN the same user becomes anonymous with a new code due`() =
        runTest {
            // Given
            prepareScenario(currentUser = appleUserWithEmail)
            repository.ensureRegistered()

            // When
            val result = repository.removeSocialIdentity()

            // Then
            val account = repository.observeAccount().first()
            assertTrue(result.isSuccess)
            assertEquals(AccountStatus.Anonymous("apple-user"), account.status)
            assertEquals("P3XR7MQA2TKE", account.recoveryCode)
            assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
            assertNull(account.email)
        }

    @Test
    fun `GIVEN no internet WHEN disconnecting Google THEN fails and keeps the account as it was`() = runTest {
        // Given
        prepareScenario(currentUser = googleUserWithEmail)
        repository.ensureRegistered()
        val before = local.get()
        remote.unlinkError = IllegalStateException("offline")

        // When
        val result = repository.removeSocialIdentity()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(before, local.get())
        assertEquals(AccountStatus.Connected("user-1", AccountProvider.GOOGLE), repository.observeStatus().first())
    }

    @Test
    fun `GIVEN a Google account WHEN deleting the account THEN deletes it on the server and keeps the device data`() =
        runTest {
            // Given
            prepareScenario(currentUser = googleUserWithEmail)
            repository.ensureRegistered()
            val before = local.get()

            // When
            val result = repository.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, remote.deleteAccountCount)
            assertEquals(before, local.get())
        }

    @Test
    fun `GIVEN the server fails WHEN deleting the account THEN fails and keeps the device data`() = runTest {
        // Given
        prepareScenario(currentUser = googleUserWithEmail)
        repository.ensureRegistered()
        val before = local.get()
        remote.deleteAccountError = IllegalStateException("server error")

        // When
        val result = repository.deleteAccount()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(0, remote.deleteAccountCount)
        assertEquals(before, local.get())
    }

    private val anonymousUser =
        SessionUser(
            "anonymous",
            socialProvider = null,
            displayName = null,
            photoUrl = null,
            email = null,
        )

    private val googleUser =
        SessionUser(
            "user-1",
            socialProvider = AccountProvider.GOOGLE,
            displayName = "Pierre",
            photoUrl = null,
            email = null,
        )

    private val appleName = "Pierre Vieira"

    private val pierreEmail = "pierre@example.com"

    private val appleRelayEmail = "abc123@privaterelay.appleid.com"

    private val googleUserWithEmail = googleUser.copy(email = pierreEmail)

    private val appleUserWithEmail =
        SessionUser(
            "apple-user",
            socialProvider = AccountProvider.APPLE,
            displayName = "Pierre",
            photoUrl = null,
            email = appleRelayEmail,
        )

    // Why: a user restored with its code signs in with the hidden recovery address, which is not worth showing.
    private val restoredUserWithRecoveryEmail =
        SessionUser(
            "restored",
            socialProvider = null,
            displayName = null,
            photoUrl = null,
            email = "restored@recovery.borarachar.com",
        )

    private val appleUserWithoutName =
        SessionUser(
            "apple-user",
            socialProvider = AccountProvider.APPLE,
            displayName = null,
            photoUrl = null,
            email = null,
        )

    private val appleUserWithName =
        SessionUser(
            "apple-user",
            socialProvider = AccountProvider.APPLE,
            displayName = "Pierre",
            photoUrl = null,
            email = null,
        )

    // Why: a session flow the test emits into while mirrorSession collects it in the background.
    private val sessions = MutableSharedFlow<SessionUser?>()

    private fun TestScope.startMirroring() {
        remote.sessionUpdates = sessions
        backgroundScope.launch { repository.mirrorSession() }
        runCurrent()
    }

    private val signInCallbackUrl = "borarachar://auth-callback?code=abc"

    private fun TestScope.observeAbandonedSignIns() {
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observeAbandonedBrowserSignIn().collect {
                abandonedSignIns++
                abandoned += it
            }
        }
    }

    private suspend fun signInWithApple(
        fullName: String? = null,
        authorizationCode: String? = null,
    ): Result<ConnectResult> = repository.signInWithIdToken(
        AccountProvider.APPLE,
        idToken = "token",
        rawNonce = "nonce",
        fullName = fullName,
        authorizationCode = authorizationCode,
    )

    private val appleAuthorizationCode = "apple-authorization-code"

    private fun TestScope.startKeepingAppleTokens() {
        backgroundScope.launch { repository.keepAppleTokens() }
        runCurrent()
    }

    private fun prepareScenario(
        currentUser: SessionUser?,
        linkError: Throwable? = null,
        issueError: Throwable? = null,
        pendingLinkProvider: AccountProvider? = null,
        browserSignInProvider: AccountProvider? = null,
        browserSignInFromProfile: Boolean = false,
        callback: BrowserCallback? = null,
        browserSignInError: Throwable? = null,
        idTokenUser: SessionUser? = null,
        saveDisplayNameError: Throwable? = null,
    ) {
        local = FakeAccountLocalDataSource(
            pendingLinkProvider = pendingLinkProvider?.code,
            browserSignInProvider = browserSignInProvider?.code,
            browserSignInFromProfile = browserSignInFromProfile,
        )
        remote = FakeAccountRemoteDataSource(
            currentUser = currentUser,
            linkError = linkError,
            issueError = issueError,
            callback = callback,
            browserSignInError = browserSignInError,
            idTokenUser = idTokenUser,
        ).also { it.saveDisplayNameError = saveDisplayNameError }
        repository = AccountRepositoryImpl(
            local = local,
            remote = remote,
        )
    }
}

private class FakeAccountLocalDataSource(
    pendingLinkProvider: String?,
    browserSignInProvider: String?,
    browserSignInFromProfile: Boolean,
) : AccountLocalDataSource {
    // Why: runs once, right before the next read of the row, so a test can interleave another coroutine there.
    var beforeNextGet: (suspend () -> Unit)? = null

    // Why: a case sets it to hold reads, so a callback stays suspended midway while the app resumes.
    var reads: Deferred<Unit>? = null

    var getError: Throwable? = null

    private val row = MutableStateFlow(
        AccountEntity(
            id = "me",
            userId = null,
            provider = null,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE.code,
            pendingLinkProvider = pendingLinkProvider,
            browserSignInProvider = browserSignInProvider,
            browserSignInFromProfile = browserSignInFromProfile,
            email = null,
            signOutWhenSynced = false,
        ),
    )

    override fun observe(): Flow<AccountEntity> = row

    override suspend fun get(): AccountEntity {
        beforeNextGet?.also { beforeNextGet = null }?.invoke()
        reads?.await()
        getError?.let { throw it }
        return row.value
    }

    override suspend fun update(transform: (AccountEntity) -> AccountEntity) {
        row.value = transform(row.value)
    }

    override suspend fun clear() {
        row.value = row.value.copy(
            userId = null,
            provider = null,
            recoveryCode = null,
            recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE.code,
            pendingLinkProvider = null,
            browserSignInProvider = null,
            email = null,
            signOutWhenSynced = false,
        )
    }
}

private class FakeAccountRemoteDataSource(
    private var currentUser: SessionUser?,
    private val linkError: Throwable?,
    private val issueError: Throwable?,
    var callback: BrowserCallback?,
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
    var signOutCount = 0
    var signOutError: Throwable? = null
    var sessionUpdates: Flow<SessionUser?> = emptyFlow()
    val appleRefreshTokens = MutableSharedFlow<String>()
    val keptAppleTokens = mutableListOf<AppleToken>()
    var keepAppleTokenError: Throwable? = null
    var unlinkError: Throwable? = null
    var unlinkCount = 0
    var deleteAccountError: Throwable? = null
    var deleteAccountCount = 0

    override fun observeSessionUser(): Flow<SessionUser?> = sessionUpdates

    override fun observeAppleRefreshTokens(): Flow<String> = appleRefreshTokens

    override suspend fun getCurrentUserOrNull(): SessionUser? = currentUser ?: sessionUsers.value

    override suspend fun signInAnonymously(): SessionUser {
        anonymousSignIns++
        return SessionUser("anonymous", null, null, null, null).also { currentUser = it }
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
        currentUser = SessionUser("restored", null, null, null, null)
    }

    override suspend fun fetchRecoveryCredentials(code: String): RecoveryCredentials {
        exchangedCodes += code
        return RecoveryCredentials(
            email = "restored@recovery.borarachar.com",
            password = "secret",
        )
    }

    override suspend fun createRecoveryCode(): String {
        issueError?.let { throw it }
        return "K7QM4XPA9TRE"
    }

    override suspend fun keepAppleToken(token: AppleToken) {
        keepAppleTokenError?.let { throw it }
        keptAppleTokens += token
    }

    override suspend fun removeSocialIdentity(): String {
        unlinkError?.let { throw it }
        unlinkCount++
        currentUser = currentUser?.copy(
            socialProvider = null,
            email = null,
        )
        return "P3XR7MQA2TKE"
    }

    override suspend fun deleteAccount() {
        deleteAccountError?.let { throw it }
        deleteAccountCount++
    }

    override suspend fun signOut() {
        signOutError?.let { throw it }
        signOutCount++
        currentUser = null
        sessionUsers.value = null
    }
}
