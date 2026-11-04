package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.model.AccountPhoto
import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountPhotoUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveAccountPhotoUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var heldSession: CompletableDeferred<Unit>
    private lateinit var emitted: MutableList<AccountPhoto>
    private var sessionReads = 0

    @Test
    fun `GIVEN a local only profile WHEN observing the account photo THEN names no account and reads no session`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                sessionUser = googleUser,
            )

            // When
            val photos = emitted.toList()

            // Then
            assertEquals(
                listOf(
                    AccountPhoto(
                        provider = null,
                        photoUrl = null,
                    ),
                ),
                photos,
            )
            assertEquals(0, sessionReads)
        }

    @Test
    fun `GIVEN an anonymous account WHEN observing the account photo THEN names no account and reads no session`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
                sessionUser = googleUser,
            )

            // When
            val photos = emitted.toList()

            // Then
            assertEquals(
                listOf(
                    AccountPhoto(
                        provider = null,
                        photoUrl = null,
                    ),
                ),
                photos,
            )
            assertEquals(0, sessionReads)
        }

    @Test
    fun `GIVEN a Google account WHEN observing the account photo THEN names Google first and then its picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            val photos = emitted.toList()

            // Then
            val expected = listOf(
                AccountPhoto(
                    provider = AccountProvider.GOOGLE,
                    photoUrl = null,
                ),
                AccountPhoto(
                    provider = AccountProvider.GOOGLE,
                    photoUrl = GOOGLE_PHOTO_URL,
                ),
            )
            assertEquals(expected, photos)
        }

    @Test
    fun `GIVEN a Google session without a picture WHEN observing the account photo THEN only names Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser.copy(photoUrl = null),
            )

            // When
            val photos = emitted.toList()

            // Then
            assertEquals(
                listOf(
                    AccountPhoto(
                        provider = AccountProvider.GOOGLE,
                        photoUrl = null,
                    ),
                ),
                photos,
            )
        }

    @Test
    fun `GIVEN a Google account without a session user WHEN observing the account photo THEN only names Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = null,
            )

            // When
            val photos = emitted.toList()

            // Then
            assertEquals(
                listOf(
                    AccountPhoto(
                        provider = AccountProvider.GOOGLE,
                        photoUrl = null,
                    ),
                ),
                photos,
            )
        }

    @Test
    fun `GIVEN a Google account with a picture WHEN another Google user replaces it THEN reads the new picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )
            account.sessionUser = googleUser.copy(
                userId = "v",
                photoUrl = OTHER_GOOGLE_PHOTO_URL,
            )

            // When
            account.status = AccountStatus.Connected("v", AccountProvider.GOOGLE)

            // Then
            assertEquals(
                AccountPhoto(
                    provider = AccountProvider.GOOGLE,
                    photoUrl = OTHER_GOOGLE_PHOTO_URL,
                ),
                emitted.last(),
            )
            assertEquals(2, sessionReads)
        }

    @Test
    fun `GIVEN a Google account with a picture WHEN only its email changes THEN emits nothing new and reads once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )

            // When
            account.email = "pierre@example.com"

            // Then
            assertEquals(2, emitted.size)
            assertEquals(1, sessionReads)
        }

    @Test
    fun `GIVEN a slow session read WHEN the account disconnects meanwhile THEN names no account without waiting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
                holdsSession = true,
            )

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            assertEquals(
                AccountPhoto(
                    provider = null,
                    photoUrl = null,
                ),
                emitted.last(),
            )
        }

    @Test
    fun `GIVEN a session read dropped by a disconnection WHEN the read finishes THEN never offers its picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
                holdsSession = true,
            )
            account.status = AccountStatus.LocalOnly

            // When
            heldSession.complete(Unit)

            // Then
            val expected = listOf(
                AccountPhoto(
                    provider = AccountProvider.GOOGLE,
                    photoUrl = null,
                ),
                AccountPhoto(
                    provider = null,
                    photoUrl = null,
                ),
            )
            assertEquals(expected, emitted)
        }

    @Test
    fun `GIVEN a Google account with a picture WHEN Apple replaces it THEN names Apple and reads its picture`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = googleStatus,
                sessionUser = googleUser,
            )
            account.sessionUser = googleUser.copy(
                socialProvider = AccountProvider.APPLE,
                photoUrl = OTHER_GOOGLE_PHOTO_URL,
            )

            // When
            account.status = AccountStatus.Connected("u", AccountProvider.APPLE)

            // Then
            val expected = listOf(
                AccountPhoto(
                    provider = AccountProvider.APPLE,
                    photoUrl = null,
                ),
                AccountPhoto(
                    provider = AccountProvider.APPLE,
                    photoUrl = OTHER_GOOGLE_PHOTO_URL,
                ),
            )
            assertEquals(expected, emitted.takeLast(2))
        }

    private val googleStatus = AccountStatus.Connected("u", AccountProvider.GOOGLE)
    private val googleUser = SessionUser(
        userId = "u",
        socialProvider = AccountProvider.GOOGLE,
        displayName = "Pierre Vieira",
        photoUrl = GOOGLE_PHOTO_URL,
        email = null,
    )

    private fun TestScope.prepareScenario(
        status: AccountStatus,
        sessionUser: SessionUser?,
        holdsSession: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.sessionUser = sessionUser }
        heldSession = CompletableDeferred()
        if (!holdsSession) heldSession.complete(Unit)
        sessionReads = 0
        emitted = mutableListOf()
        val photos = ObserveAccountPhotoUseCase(
            observeAccount = ObserveAccountUseCase(account),
            getSessionUser = GetSessionUser {
                sessionReads++
                heldSession.await()
                account.sessionUser
            },
        )()
        backgroundScope.launch { photos.collect(emitted::add) }
    }

    private companion object {
        const val GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/pierre"
        const val OTHER_GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/ana"
    }
}
