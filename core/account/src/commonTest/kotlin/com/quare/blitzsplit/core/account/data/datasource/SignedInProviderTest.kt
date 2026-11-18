package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.model.AccountProvider
import io.github.jan.supabase.auth.user.Identity
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignedInProviderTest {
    @Test
    fun `GIVEN a user without identities WHEN reading the provider THEN there is none`() {
        // Given
        val identities = listOf<List<Identity>?>(null, emptyList())

        // When
        val providers = identities.map(::firstRanked)

        // Then
        assertEquals(listOf<AccountProvider?>(null, null), providers)
    }

    @Test
    fun `GIVEN only a Google identity WHEN reading the provider THEN it is Google`() {
        // Given
        val identities = listOf(google(lastSignInAt = "2026-11-10T10:00:00Z"))

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.GOOGLE, provider)
    }

    @Test
    fun `GIVEN only an Apple identity WHEN reading the provider THEN it is Apple`() {
        // Given
        val identities = listOf(apple(lastSignInAt = "2026-11-10T10:00:00Z"))

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.APPLE, provider)
    }

    @Test
    fun `GIVEN Apple linked first and Google signed in last WHEN reading the provider THEN it is Google`() {
        // Given
        val identities = listOf(
            apple(lastSignInAt = "2026-11-01T10:00:00Z"),
            google(lastSignInAt = "2026-11-18T09:30:00Z"),
        )

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.GOOGLE, provider)
    }

    @Test
    fun `GIVEN Google linked first and Apple signed in last WHEN reading the provider THEN it is Apple`() {
        // Given
        val identities = listOf(
            google(lastSignInAt = "2026-11-01T10:00:00Z"),
            apple(lastSignInAt = "2026-11-18T09:30:00Z"),
        )

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.APPLE, provider)
    }

    @Test
    fun `GIVEN both identities listed in either order WHEN reading the provider THEN the order does not matter`() {
        // Given
        val google = google(lastSignInAt = "2026-11-18T09:30:00.123456Z")
        val apple = apple(lastSignInAt = "2026-11-18T09:30:00.123455Z")

        // When
        val providers = listOf(listOf(google, apple), listOf(apple, google)).map(::firstRanked)

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE, AccountProvider.GOOGLE), providers)
    }

    @Test
    fun `GIVEN a newer identity of another provider WHEN reading the provider THEN only Google and Apple count`() {
        // Given
        val identities = listOf(
            apple(lastSignInAt = "2026-11-01T10:00:00Z"),
            identity(
                provider = "email",
                lastSignInAt = "2026-11-18T09:30:00Z",
            ),
        )

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.APPLE, provider)
    }

    @Test
    fun `GIVEN only an identity of another provider WHEN reading the provider THEN there is none`() {
        // Given
        val identities = listOf(
            identity(
                provider = "email",
                lastSignInAt = "2026-11-18T09:30:00Z",
            ),
        )

        // When
        val provider = firstRanked(identities)

        // Then
        assertNull(provider)
    }

    @Test
    fun `GIVEN identities without a readable sign in time WHEN reading the provider THEN the one with a time wins`() {
        // Given
        val identities = listOf(
            apple(lastSignInAt = null),
            google(lastSignInAt = "not a time"),
            apple(lastSignInAt = "2026-11-18T09:30:00Z"),
        )

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.APPLE, provider)
    }

    @Test
    fun `GIVEN identities without any sign in time WHEN reading the provider THEN the first listed wins`() {
        // Given
        val identities = listOf(google(lastSignInAt = null), apple(lastSignInAt = null))

        // When
        val provider = firstRanked(identities)

        // Then
        assertEquals(AccountProvider.GOOGLE, provider)
    }

    private fun firstRanked(identities: List<Identity>?): AccountProvider? =
        SignedInProvider.toRankedProviders(identities).firstOrNull()

    private fun google(lastSignInAt: String?): Identity = identity(
        provider = "google",
        lastSignInAt = lastSignInAt,
    )

    private fun apple(lastSignInAt: String?): Identity = identity(
        provider = "apple",
        lastSignInAt = lastSignInAt,
    )

    private fun identity(
        provider: String,
        lastSignInAt: String?,
    ): Identity = Identity(
        id = "$provider-id",
        identityData = JsonObject(emptyMap()),
        lastSignInAt = lastSignInAt,
        provider = provider,
        userId = "user-1",
    )

    @Test
    fun `GIVEN both identities WHEN ranking THEN lists the latest sign in first and keeps the other after it`() {
        // Given
        val identities = listOf(
            google(lastSignInAt = "2026-11-01T10:00:00Z"),
            apple(lastSignInAt = "2026-11-18T09:30:00Z"),
            identity(
                provider = "email",
                lastSignInAt = "2026-11-20T09:30:00Z",
            ),
        )

        // When
        val ranked = SignedInProvider.toRankedProviders(identities)

        // Then
        assertEquals(listOf(AccountProvider.APPLE, AccountProvider.GOOGLE), ranked)
    }
}
