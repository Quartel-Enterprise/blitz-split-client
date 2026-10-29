package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.data.mapper.UserDataExportMapperImpl
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ExportUserDataUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportUserDataUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var useCase: ExportUserDataUseCase

    @Test
    fun `GIVEN any device WHEN exporting THEN names the format and its version`() = runTest {
        // Given
        prepareScenario()

        // When
        val export = parse(useCase().getOrThrow())

        // Then
        assertEquals("blitz-split-user-data", export.getValue("format").jsonPrimitive.content)
        assertEquals(2, export.getValue("version").jsonPrimitive.int)
    }

    @Test
    fun `GIVEN the current time WHEN exporting THEN dates the export with it as an ISO-8601 instant`() = runTest {
        // Given
        prepareScenario()

        // When
        val export = parse(useCase().getOrThrow())

        // Then
        val exportedAt = export.getValue("exportedAt").jsonPrimitive.content
        assertEquals(NOW, Instant.parse(exportedAt).toEpochMilliseconds())
        assertTrue(exportedAt.startsWith("2026-01-01T00:00:00.123"))
    }

    @Test
    fun `GIVEN any device WHEN exporting THEN the JSON is pretty printed`() = runTest {
        // Given
        prepareScenario()

        // When
        val json = useCase().getOrThrow()

        // Then
        assertTrue(json.lines().size > 1)
        assertTrue(json.lines().any { it.startsWith("    \"") })
    }

    @Test
    fun `GIVEN a device with no account WHEN exporting THEN the account is none with no id and no email`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val exported = parse(useCase().getOrThrow()).getValue("account").jsonObject

        // Then
        assertEquals("none", exported.getValue("type").jsonPrimitive.content)
        assertEquals(JsonNull, exported.getValue("userId"))
        assertEquals(JsonNull, exported.getValue("email"))
    }

    @Test
    fun `GIVEN an anonymous user WHEN exporting THEN the account is anonymous with its id and no email`() = runTest {
        // Given
        prepareScenario(status = anonymous)

        // When
        val exported = parse(useCase().getOrThrow()).getValue("account").jsonObject

        // Then
        assertEquals("anonymous", exported.getValue("type").jsonPrimitive.content)
        assertEquals(FakeAccountRepository.ANONYMOUS_USER_ID, exported.getValue("userId").jsonPrimitive.content)
        assertEquals(JsonNull, exported.getValue("email"))
    }

    @Test
    fun `GIVEN a Google account WHEN exporting THEN the account is google with its id and email`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
            email = "pierre@gmail.com",
        )

        // When
        val exported = parse(useCase().getOrThrow()).getValue("account").jsonObject

        // Then
        assertEquals("google", exported.getValue("type").jsonPrimitive.content)
        assertEquals(FakeAccountRepository.CONNECTED_USER_ID, exported.getValue("userId").jsonPrimitive.content)
        assertEquals("pierre@gmail.com", exported.getValue("email").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN an Apple account WHEN exporting THEN the account is apple with its id and email`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE),
            email = "pierre@privaterelay.appleid.com",
        )

        // When
        val exported = parse(useCase().getOrThrow()).getValue("account").jsonObject

        // Then
        assertEquals("apple", exported.getValue("type").jsonPrimitive.content)
        assertEquals(FakeAccountRepository.CONNECTED_USER_ID, exported.getValue("userId").jsonPrimitive.content)
        assertEquals("pierre@privaterelay.appleid.com", exported.getValue("email").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a profile with an uploaded photo WHEN exporting THEN exports its name color and photo URL`() = runTest {
        // Given
        prepareScenario(
            storedProfile = UserProfile(
                displayName = "Pierre",
                avatarColor = AvatarColor.LILAC,
                photo = ProfilePhoto.Remote(PHOTO_URL),
            ),
        )

        // When
        val exported = parse(useCase().getOrThrow()).getValue("profile").jsonObject

        // Then
        assertEquals("Pierre", exported.getValue("displayName").jsonPrimitive.content)
        assertEquals("lilac", exported.getValue("avatarColor").jsonPrimitive.content)
        assertEquals(PHOTO_URL, exported.getValue("photoUrl").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a photo picked offline and not uploaded yet WHEN exporting THEN the photo URL is null`() = runTest {
        // Given
        prepareScenario(
            storedProfile = UserProfile(
                displayName = "Pierre",
                avatarColor = AvatarColor.GREEN,
                photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3)),
            ),
        )

        // When
        val exported = parse(useCase().getOrThrow()).getValue("profile").jsonObject

        // Then
        assertEquals(JsonNull, exported.getValue("photoUrl"))
        assertEquals("green", exported.getValue("avatarColor").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a profile with no photo WHEN exporting THEN the photo URL is null`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        val exported = parse(useCase().getOrThrow()).getValue("profile").jsonObject

        // Then
        assertEquals(JsonNull, exported.getValue("photoUrl"))
    }

    @Test
    fun `GIVEN no profile WHEN exporting THEN the profile is null`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        val export = parse(useCase().getOrThrow())

        // Then
        assertEquals(JsonNull, export.getValue("profile"))
    }

    @Test
    fun `GIVEN a name with spaces around it WHEN exporting THEN exports the trimmed name`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre.copy(displayName = "  Pierre  "))

        // When
        val exported = parse(useCase().getOrThrow()).getValue("profile").jsonObject

        // Then
        assertEquals("Pierre", exported.getValue("displayName").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a Pix and a Bitcoin key WHEN exporting THEN exports each with its method type value default and date`() =
        runTest {
            // Given
            prepareScenario(storedKeys = listOf(pixKey, lightningKey))

            // When
            val keys = parse(useCase().getOrThrow()).getValue("paymentKeys").jsonArray.map { it.jsonObject }

            // Then
            assertEquals(2, keys.size)
            val pix = keys.single { it.getValue("value").jsonPrimitive.content == pixKey.value }
            assertEquals("pix", pix.getValue("method").jsonPrimitive.content)
            assertEquals("email", pix.getValue("type").jsonPrimitive.content)
            assertEquals(true, pix.getValue("isDefault").jsonPrimitive.boolean)
            assertEquals(
                pixKey.addedAt,
                Instant.parse(pix.getValue("addedAt").jsonPrimitive.content).toEpochMilliseconds(),
            )
            val lightning = keys.single { it.getValue("value").jsonPrimitive.content == lightningKey.value }
            assertEquals("bitcoin", lightning.getValue("method").jsonPrimitive.content)
            assertEquals("lightning", lightning.getValue("type").jsonPrimitive.content)
            assertEquals(false, lightning.getValue("isDefault").jsonPrimitive.boolean)
            assertEquals(
                lightningKey.addedAt,
                Instant.parse(lightning.getValue("addedAt").jsonPrimitive.content).toEpochMilliseconds(),
            )
        }

    @Test
    fun `GIVEN a CPF a phone a random and an onchain key WHEN exporting THEN exports their types in lower case`() =
        runTest {
            // Given
            prepareScenario(
                storedKeys = listOf(
                    key(
                        id = "cpf",
                        type = PaymentKeyType.CPF,
                    ),
                    key(
                        id = "phone",
                        type = PaymentKeyType.PHONE,
                    ),
                    key(
                        id = "random",
                        type = PaymentKeyType.RANDOM,
                    ),
                    key(
                        id = "onchain",
                        type = PaymentKeyType.ONCHAIN,
                    ),
                ),
            )

            // When
            val keys = parse(useCase().getOrThrow()).getValue("paymentKeys").jsonArray.map { it.jsonObject }

            // Then
            assertEquals(
                listOf("pix" to "cpf", "pix" to "phone", "pix" to "random", "bitcoin" to "onchain"),
                keys.map { it.getValue("method").jsonPrimitive.content to it.getValue("type").jsonPrimitive.content },
            )
        }

    @Test
    fun `GIVEN a key deleted on this device WHEN exporting THEN leaves it out`() = runTest {
        // Given
        prepareScenario(storedKeys = listOf(pixKey, lightningKey))
        paymentKeys.deleteKey(
            id = pixKey.id,
            nextDefault = null,
        )

        // When
        val keys = parse(useCase().getOrThrow()).getValue("paymentKeys").jsonArray.map { it.jsonObject }

        // Then
        assertEquals(listOf(lightningKey.value), keys.map { it.getValue("value").jsonPrimitive.content })
    }

    @Test
    fun `GIVEN no keys WHEN exporting THEN the payment keys are an empty list`() = runTest {
        // Given
        prepareScenario(storedKeys = emptyList())

        // When
        val export = parse(useCase().getOrThrow())

        // Then
        assertTrue(export.getValue("paymentKeys").jsonArray.isEmpty())
    }

    @Test
    fun `GIVEN an anonymous user WHEN exporting THEN reads only the device copy and reaches no server`() = runTest {
        // Given
        prepareScenario(
            status = anonymous,
            storedKeys = listOf(pixKey),
        )

        // When
        useCase()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(profile.downloadedUserIds.isEmpty())
        assertTrue(paymentKeys.downloadedUserIds.isEmpty())
    }

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val pixKey = key(
        id = "pix-email",
        type = PaymentKeyType.EMAIL,
        value = "pierre@example.com",
        isDefault = true,
        addedAt = 1_760_000_000_000L,
    )
    private val lightningKey = key(
        id = "lightning",
        type = PaymentKeyType.LIGHTNING,
        value = "pierre@walletofsatoshi.com",
        isDefault = false,
        addedAt = 1_760_000_000_456L,
    )

    private fun key(
        id: String,
        type: PaymentKeyType,
        value: String = "value-$id",
        isDefault: Boolean = false,
        addedAt: Long = 1L,
    ): PaymentKey = PaymentKey(
        id = id,
        type = type,
        value = value,
        isDefault = isDefault,
        addedAt = addedAt,
    )

    private fun parse(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun prepareScenario(
        status: AccountStatus = AccountStatus.LocalOnly,
        email: String? = null,
        storedProfile: UserProfile? = pierre,
        storedKeys: List<PaymentKey> = emptyList(),
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.email = email }
        profile = FakeProfileRepository(initialProfile = storedProfile)
        paymentKeys = FakePaymentKeyRepository(initialKeys = storedKeys)
        useCase = ExportUserDataUseCase(
            accountRepository = account,
            profileRepository = profile,
            paymentKeyRepository = paymentKeys,
            notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
            mapper = UserDataExportMapperImpl(),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        // Why: 2026-01-01T00:00:00.123Z, with milliseconds so a dropped fraction shows.
        const val NOW = 1_767_225_600_123L
        const val PHOTO_URL = "https://example.supabase.co/storage/v1/object/public/avatars/pierre.jpg"
    }
}
