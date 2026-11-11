package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.usecase.impl.GetLegalLanguageUseCase
import com.quare.blitzsplit.core.model.AppLanguage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetLegalLanguageUseCaseTest {
    private lateinit var getLegalLanguage: GetLegalLanguageUseCase

    @Test
    fun `GIVEN Portuguese picked in Profile on a Japanese device WHEN asking the language THEN it is pt`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.PORTUGUESE,
            device = "ja-JP",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("pt", language)
    }

    @Test
    fun `GIVEN English picked in Profile on a Portuguese device WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.ENGLISH,
            device = "pt-BR",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN Japanese picked in Profile and no device language WHEN asking the language THEN it is ja`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.JAPANESE,
            device = null,
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("ja", language)
    }

    @Test
    fun `GIVEN System on a pt-BR device WHEN asking the language THEN it is pt`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "pt-BR",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("pt", language)
    }

    @Test
    fun `GIVEN System on an es-ES device WHEN asking the language THEN it is es`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "es-ES",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("es", language)
    }

    @Test
    fun `GIVEN System on a device that names Indonesian by its old code WHEN asking the language THEN it is id`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "in-ID",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("id", language)
        }

    @Test
    fun `GIVEN System on a zh-Hans-CN device WHEN asking the language THEN it is zh`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "zh-Hans-CN",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("zh", language)
    }

    @Test
    fun `GIVEN System on a device in a language the app lacks WHEN asking the language THEN it is that language`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "tr",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("tr", language)
        }

    @Test
    fun `GIVEN System on a device with an upper case language WHEN asking the language THEN it is lower case`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "PT-br",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("pt", language)
        }

    @Test
    fun `GIVEN System on a device with an underscore in its language WHEN asking the language THEN it is pt`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "pt_BR",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("pt", language)
        }

    @Test
    fun `GIVEN System on a device with a three letter language WHEN asking the language THEN it is that language`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "fil-PH",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("fil", language)
        }

    @Test
    fun `GIVEN System and no device language WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = null,
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and an empty device language WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and a blank device language WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "  ",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and a device language that starts with a dash WHEN asking the language THEN it is en`() =
        runTest {
            // Given
            prepareScenario(
                picked = AppLanguage.SYSTEM,
                device = "-BR",
            )

            // When
            val language = getLegalLanguage()

            // Then
            assertEquals("en", language)
        }

    @Test
    fun `GIVEN System and a device language that is a path WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "../x",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and a device language of digits WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "12-BR",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and a device language of one letter WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "c",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    @Test
    fun `GIVEN System and a device language of too many letters WHEN asking the language THEN it is en`() = runTest {
        // Given
        prepareScenario(
            picked = AppLanguage.SYSTEM,
            device = "portuguese",
        )

        // When
        val language = getLegalLanguage()

        // Then
        assertEquals("en", language)
    }

    private fun prepareScenario(
        picked: AppLanguage,
        device: String?,
    ) {
        getLegalLanguage = GetLegalLanguageUseCase(
            observeAppLanguage = { flowOf(picked) },
            deviceLanguage = { device },
        )
    }
}
