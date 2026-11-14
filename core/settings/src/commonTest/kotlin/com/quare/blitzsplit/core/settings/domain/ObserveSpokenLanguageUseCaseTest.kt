package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.repository.AppLanguageRepositoryImpl
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveSpokenLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateAppLanguageUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveSpokenLanguageUseCaseTest {
    private lateinit var updateAppLanguage: UpdateAppLanguageUseCase
    private lateinit var observeSpokenLanguage: ObserveSpokenLanguageUseCase

    // Why: what the device reports; a test of several devices sets it between two readings.
    private var deviceLanguage: String? = null

    @Test
    fun `GIVEN Portuguese picked in Profile on a Japanese device WHEN observing the spoken language THEN it is pt`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.PORTUGUESE,
                device = "ja-JP",
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("pt", spoken)
        }

    @Test
    fun `GIVEN English picked in Profile on a Portuguese device WHEN observing the spoken language THEN it is en`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.ENGLISH,
                device = "pt-BR",
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("en", spoken)
        }

    @Test
    fun `GIVEN Turkish picked in Profile on a Chinese device WHEN observing the spoken language THEN it is tr`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.TURKISH,
                device = "zh-CN",
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("tr", spoken)
        }

    @Test
    fun `GIVEN Indonesian picked in Profile and no device language WHEN observing the spoken language THEN it is id`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.INDONESIAN,
                device = null,
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("id", spoken)
        }

    @Test
    fun `GIVEN Spanish picked in Profile on a device in Spain WHEN observing the spoken language THEN it is es`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SPANISH,
                device = "es-ES",
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("es", spoken)
        }

    @Test
    fun `GIVEN a Catalan device WHEN picking each language of the app THEN it is the code of the one picked`() =
        runTest {
            // Given
            prepareScenario(device = "ca-ES")
            val picks = listOf(
                AppLanguage.ENGLISH,
                AppLanguage.PORTUGUESE,
                AppLanguage.SPANISH,
                AppLanguage.FRENCH,
                AppLanguage.GERMAN,
                AppLanguage.ITALIAN,
                AppLanguage.POLISH,
                AppLanguage.DUTCH,
                AppLanguage.JAPANESE,
                AppLanguage.TURKISH,
                AppLanguage.INDONESIAN,
            )

            // When
            val spoken = picks.map { spokenWith(it) }

            // Then
            assertEquals(listOf("en", "pt", "es", "fr", "de", "it", "pl", "nl", "ja", "tr", "id"), spoken)
        }

    @Test
    fun `GIVEN System on a pt-BR device WHEN observing the spoken language THEN it is pt`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "pt-BR",
        )

        // When
        val spoken = observeSpokenLanguage().first()

        // Then
        assertEquals("pt", spoken)
    }

    @Test
    fun `GIVEN System WHEN the device is in each language of the app THEN it is that language`() = runTest {
        // Given
        prepareScenario(picked = AppLanguage.SYSTEM)
        val devices = listOf("en", "pt", "es", "fr", "de", "it", "pl", "nl", "ja", "tr", "id")

        // When
        val spoken = devices.map { spokenOn(it) }

        // Then
        assertEquals(listOf("en", "pt", "es", "fr", "de", "it", "pl", "nl", "ja", "tr", "id"), spoken)
    }

    @Test
    fun `GIVEN System WHEN the device is in a regional variant of a language of the app THEN it is that language`() =
        runTest {
            // Given
            prepareScenario(picked = AppLanguage.SYSTEM)
            val devices = listOf(
                "pt-PT",
                "pt-BR",
                "es-ES",
                "es-MX",
                "es-419",
                "en-GB",
                "en-US",
                "fr-CA",
                "de-AT",
                "it-CH",
                "pl-PL",
                "nl-BE",
                "ja-JP",
                "tr-CY",
                "id-ID",
            )

            // When
            val spoken = devices.map { spokenOn(it) }

            // Then
            assertEquals(
                listOf("pt", "pt", "es", "es", "es", "en", "en", "fr", "de", "it", "pl", "nl", "ja", "tr", "id"),
                spoken,
            )
        }

    @Test
    fun `GIVEN System WHEN the device tag carries a script or its own preferences THEN it is only the language`() =
        runTest {
            // Given
            prepareScenario(picked = AppLanguage.SYSTEM)
            val devices = listOf("pt-BR-u-mu-celsius", "en-US-u-fw-mon", "ja-JP-u-ca-japanese", "es-Latn-ES")

            // When
            val spoken = devices.map { spokenOn(it) }

            // Then
            assertEquals(listOf("pt", "en", "ja", "es"), spoken)
        }

    @Test
    fun `GIVEN System WHEN the device writes its tag in upper case or with an underscore THEN it is lower case`() =
        runTest {
            // Given
            prepareScenario(picked = AppLanguage.SYSTEM)
            val devices = listOf("PT-BR", "pt_BR", "PT_br", "ES", "En-gb", "TR")

            // When
            val spoken = devices.map { spokenOn(it) }

            // Then
            assertEquals(listOf("pt", "pt", "pt", "es", "en", "tr"), spoken)
        }

    @Test
    fun `GIVEN System WHEN the device names Indonesian by the legacy code of Android THEN it is id`() = runTest {
        // Given
        prepareScenario(picked = AppLanguage.SYSTEM)
        val devices = listOf("in", "in-ID", "in_ID", "IN")

        // When
        val spoken = devices.map { spokenOn(it) }

        // Then
        assertEquals(listOf("id", "id", "id", "id"), spoken)
    }

    @Test
    fun `GIVEN System WHEN the device is in a language the app does not have THEN it is en`() = runTest {
        // Given
        prepareScenario(picked = AppLanguage.SYSTEM)
        val devices = listOf("ca", "ca-ES", "zh", "zh-CN", "zh-Hans-CN", "zh-Hant-TW", "fil", "fil-PH", "ko-KR")

        // When
        val spoken = devices.map { spokenOn(it) }

        // Then
        assertEquals(listOf("en", "en", "en", "en", "en", "en", "en", "en", "en"), spoken)
    }

    @Test
    fun `GIVEN System WHEN the code of the device language only starts like one the app has THEN it is en`() = runTest {
        // Given
        prepareScenario(picked = AppLanguage.SYSTEM)
        val devices = listOf("frr-DE", "esu-US", "jam-JM", "ptx")

        // When
        val spoken = devices.map { spokenOn(it) }

        // Then
        assertEquals(listOf("en", "en", "en", "en"), spoken)
    }

    @Test
    fun `GIVEN System and a device that reports no language WHEN observing the spoken language THEN it is en`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = null,
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("en", spoken)
        }

    @Test
    fun `GIVEN System and a device that reports an empty language WHEN observing the spoken language THEN it is en`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "",
            )

            // When
            val spoken = observeSpokenLanguage().first()

            // Then
            assertEquals("en", spoken)
        }

    @Test
    fun `GIVEN System WHEN the device reports a malformed language THEN it is en`() = runTest {
        // Given
        prepareScenario(picked = AppLanguage.SYSTEM)
        val devices = listOf(" ", "-", "_", "-BR", "_BR", "und", "p", "123", "portuguese")

        // When
        val spoken = devices.map { spokenOn(it) }

        // Then
        assertEquals(listOf("en", "en", "en", "en", "en", "en", "en", "en", "en"), spoken)
    }

    @Test
    fun `GIVEN System on a Portuguese device being observed WHEN Spanish is picked THEN observers see es`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "pt-BR",
        )
        val observed = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeSpokenLanguage().collect(observed::add)
        }

        // When
        updateAppLanguage(AppLanguage.SPANISH)

        // Then
        assertEquals(listOf("pt", "es"), observed)
    }

    @Test
    fun `GIVEN Spanish picked on a Portuguese device being observed WHEN going back to System THEN observers see pt`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SPANISH,
                device = "pt-BR",
            )
            val observed = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeSpokenLanguage().collect(observed::add)
            }

            // When
            updateAppLanguage(AppLanguage.SYSTEM)

            // Then
            assertEquals(listOf("es", "pt"), observed)
        }

    @Test
    fun `GIVEN Japanese picked on a Chinese device being observed WHEN going back to System THEN observers see en`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.JAPANESE,
                device = "zh-CN",
            )
            val observed = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeSpokenLanguage().collect(observed::add)
            }

            // When
            updateAppLanguage(AppLanguage.SYSTEM)

            // Then
            assertEquals(listOf("ja", "en"), observed)
        }

    private suspend fun spokenWith(pick: AppLanguage): String {
        updateAppLanguage(pick)
        return observeSpokenLanguage().first()
    }

    private suspend fun spokenOn(device: String?): String {
        deviceLanguage = device
        return observeSpokenLanguage().first()
    }

    private fun prepareScenario(
        picked: AppLanguage = AppLanguage.SYSTEM,
        device: String? = "en-US",
    ) {
        deviceLanguage = device
        val repository = AppLanguageRepositoryImpl(FakeAppLanguageLocalDataSource(picked))
        updateAppLanguage = UpdateAppLanguageUseCase(repository)
        observeSpokenLanguage = ObserveSpokenLanguageUseCase(
            observeAppLanguage = ObserveAppLanguageUseCase(repository),
            deviceLanguage = { deviceLanguage },
        )
    }
}
