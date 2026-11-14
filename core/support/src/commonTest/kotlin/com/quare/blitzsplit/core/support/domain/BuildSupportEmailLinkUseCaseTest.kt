package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.core.support.domain.usecase.impl.BuildSupportEmailLinkUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildSupportEmailLinkUseCaseTest {
    private val useCase = BuildSupportEmailLinkUseCase()

    @Test
    fun `GIVEN any diagnostics WHEN building the link THEN it is a mailto to the support address as it is`() {
        // Given
        val diagnostics = diagnostics()

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.startsWith("mailto:appborarachar@gmail.com?subject="), link)
    }

    @Test
    fun `GIVEN every diagnostic and a profile link WHEN building the link THEN it is the whole encoded email`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "1.4.0",
            platform = SupportPlatform.ANDROID,
            language = AppLanguage.PORTUGUESE,
            account = SupportAccount.GOOGLE,
            profileLink = "https://blitzsplit.com/u/7Hk2pQ8xZa",
        )

        // When
        val link = useCase(diagnostics)

        // Then
        assertEquals(
            "mailto:appborarachar@gmail.com" +
                "?subject=Blitz%20Split%20support%20%28Android%201.4.0%29" +
                "&body=%0D%0A%0D%0A---%0D%0A" +
                "App%20version%3A%201.4.0%0D%0A" +
                "Platform%3A%20Android%0D%0A" +
                "Language%3A%20pt-BR%0D%0A" +
                "Account%3A%20Google%0D%0A" +
                "Profile%3A%20https%3A%2F%2Fblitzsplit.com%2Fu%2F7Hk2pQ8xZa",
            link,
        )
    }

    @Test
    fun `GIVEN no profile link WHEN building the link THEN the body ends at the account line`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "1.4.0",
            platform = SupportPlatform.WEB,
            language = AppLanguage.ENGLISH,
            account = SupportAccount.ANONYMOUS,
            profileLink = null,
        )

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertEquals(
            "%0D%0A%0D%0A---%0D%0A" +
                "App%20version%3A%201.4.0%0D%0A" +
                "Platform%3A%20Web%0D%0A" +
                "Language%3A%20en%0D%0A" +
                "Account%3A%20Anonymous",
            body,
        )
    }

    @Test
    fun `GIVEN the Android app WHEN building the link THEN the subject names Android and the version`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "2.0.3",
            platform = SupportPlatform.ANDROID,
        )

        // When
        val subject = subjectOf(useCase(diagnostics))

        // Then
        assertEquals("Blitz%20Split%20support%20%28Android%202.0.3%29", subject)
    }

    @Test
    fun `GIVEN the iOS app WHEN building the link THEN the subject names iOS and the version`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "2.0.3",
            platform = SupportPlatform.IOS,
        )

        // When
        val subject = subjectOf(useCase(diagnostics))

        // Then
        assertEquals("Blitz%20Split%20support%20%28iOS%202.0.3%29", subject)
    }

    @Test
    fun `GIVEN the Mac app WHEN building the link THEN the subject names macOS and the version`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "2.0.3",
            platform = SupportPlatform.MACOS,
        )

        // When
        val subject = subjectOf(useCase(diagnostics))

        // Then
        assertEquals("Blitz%20Split%20support%20%28macOS%202.0.3%29", subject)
    }

    @Test
    fun `GIVEN the web app WHEN building the link THEN the subject names Web and the version`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "2.0.3",
            platform = SupportPlatform.WEB,
        )

        // When
        val subject = subjectOf(useCase(diagnostics))

        // Then
        assertEquals("Blitz%20Split%20support%20%28Web%202.0.3%29", subject)
    }

    @Test
    fun `GIVEN the app in Portuguese WHEN building the link THEN the subject is still in English`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "1.4.0",
            language = AppLanguage.PORTUGUESE,
        )

        // When
        val subject = subjectOf(useCase(diagnostics))

        // Then
        assertEquals("Blitz%20Split%20support%20%28Android%201.4.0%29", subject)
    }

    @Test
    fun `GIVEN the iOS app WHEN building the link THEN the body has the iOS platform line`() {
        // Given
        val diagnostics = diagnostics(platform = SupportPlatform.IOS)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.contains("%0D%0APlatform%3A%20iOS%0D%0A"), body)
    }

    @Test
    fun `GIVEN the Mac app WHEN building the link THEN the body has the macOS platform line`() {
        // Given
        val diagnostics = diagnostics(platform = SupportPlatform.MACOS)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.contains("%0D%0APlatform%3A%20macOS%0D%0A"), body)
    }

    @Test
    fun `GIVEN the app follows the device WHEN building the link THEN the language says System and its language`() {
        // Given
        val diagnostics = diagnostics(
            language = AppLanguage.SYSTEM,
            spokenLanguage = "pt",
        )

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.contains("%0D%0ALanguage%3A%20System%20%28pt%29%0D%0A"), body)
    }

    @Test
    fun `GIVEN the app in Japanese WHEN building the link THEN the language line has its tag`() {
        // Given
        val diagnostics = diagnostics(language = AppLanguage.JAPANESE)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.contains("%0D%0ALanguage%3A%20ja%0D%0A"), body)
    }

    @Test
    fun `GIVEN a local only install WHEN building the link THEN the account line says Local only`() {
        // Given
        val diagnostics = diagnostics(account = SupportAccount.LOCAL_ONLY)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.endsWith("%0D%0AAccount%3A%20Local%20only"), body)
    }

    @Test
    fun `GIVEN an anonymous account WHEN building the link THEN the account line says Anonymous`() {
        // Given
        val diagnostics = diagnostics(account = SupportAccount.ANONYMOUS)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.endsWith("%0D%0AAccount%3A%20Anonymous"), body)
    }

    @Test
    fun `GIVEN a Google account WHEN building the link THEN the account line says Google`() {
        // Given
        val diagnostics = diagnostics(account = SupportAccount.GOOGLE)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.endsWith("%0D%0AAccount%3A%20Google"), body)
    }

    @Test
    fun `GIVEN an Apple account WHEN building the link THEN the account line says Apple`() {
        // Given
        val diagnostics = diagnostics(account = SupportAccount.APPLE)

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.endsWith("%0D%0AAccount%3A%20Apple"), body)
    }

    @Test
    fun `GIVEN any diagnostics WHEN building the link THEN the body starts with two empty lines and the rule`() {
        // Given
        val diagnostics = diagnostics()

        // When
        val body = bodyOf(useCase(diagnostics))

        // Then
        assertTrue(body.startsWith("%0D%0A%0D%0A---%0D%0AApp%20version%3A%20"), body)
    }

    @Test
    fun `GIVEN reserved characters in the diagnostics WHEN building the link THEN only its separators are left raw`() {
        // Given
        val diagnostics = diagnostics(
            appVersion = "1.4.0?a=b&c",
            profileLink = "https://blitzsplit.com/u/7Hk2pQ8xZa?ref=mail&lang=pt#top",
        )

        // When
        val link = useCase(diagnostics)

        // Then
        assertEquals(1, link.count { it == '?' })
        assertEquals(1, link.count { it == '&' })
        assertEquals(2, link.count { it == '=' })
        assertEquals(1, link.count { it == ':' })
        assertFalse(link.contains('/'))
        assertFalse(link.contains('#'))
        assertFalse(link.contains('('))
        assertFalse(link.contains(')'))
        assertTrue(link.endsWith("%3Fref%3Dmail%26lang%3Dpt%23top"), link)
    }

    @Test
    fun `GIVEN a space and a plus sign in the version WHEN building the link THEN both are percent encoded`() {
        // Given
        val diagnostics = diagnostics(appVersion = "1.4.0 beta+3")

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.contains("App%20version%3A%201.4.0%20beta%2B3%0D%0A"), link)
        assertFalse(link.contains('+'))
        assertFalse(link.contains(' '))
    }

    @Test
    fun `GIVEN any diagnostics WHEN building the link THEN no raw line break is left in it`() {
        // Given
        val diagnostics = diagnostics(profileLink = "https://blitzsplit.com/u/7Hk2pQ8xZa")

        // When
        val link = useCase(diagnostics)

        // Then
        assertFalse(link.contains('\n'))
        assertFalse(link.contains('\r'))
        assertFalse(link.replace("%0D%0A", "").contains("%0A"))
        assertFalse(link.replace("%0D%0A", "").contains("%0D"))
    }

    @Test
    fun `GIVEN unreserved characters in the version WHEN building the link THEN they stay as they are`() {
        // Given
        val diagnostics = diagnostics(appVersion = "1.4.0-rc_2~Zz9")

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.contains("%28Android%201.4.0-rc_2~Zz9%29"), link)
        assertTrue(link.contains("App%20version%3A%201.4.0-rc_2~Zz9%0D%0A"), link)
    }

    @Test
    fun `GIVEN a two byte character in the version WHEN building the link THEN each UTF-8 byte is encoded`() {
        // Given
        val diagnostics = diagnostics(appVersion = "1.4.0-é")

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.contains("App%20version%3A%201.4.0-%C3%A9%0D%0A"), link)
    }

    @Test
    fun `GIVEN a three byte character in the version WHEN building the link THEN each UTF-8 byte is encoded`() {
        // Given
        val diagnostics = diagnostics(appVersion = "1.4.0-日")

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.contains("App%20version%3A%201.4.0-%E6%97%A5%0D%0A"), link)
    }

    @Test
    fun `GIVEN a four byte character in the version WHEN building the link THEN each UTF-8 byte is encoded`() {
        // Given
        val diagnostics = diagnostics(appVersion = "1.4.0-😀")

        // When
        val link = useCase(diagnostics)

        // Then
        assertTrue(link.contains("App%20version%3A%201.4.0-%F0%9F%98%80%0D%0A"), link)
    }

    private fun subjectOf(link: String): String = link.substringAfter("?subject=").substringBefore("&body=")

    private fun bodyOf(link: String): String = link.substringAfter("&body=")

    private fun diagnostics(
        appVersion: String = "1.4.0",
        platform: SupportPlatform = SupportPlatform.ANDROID,
        language: AppLanguage = AppLanguage.ENGLISH,
        account: SupportAccount = SupportAccount.LOCAL_ONLY,
        profileLink: String? = null,
        spokenLanguage: String = "en",
    ): SupportDiagnostics = SupportDiagnostics(
        appVersion = appVersion,
        platform = platform,
        language = language,
        spokenLanguage = spokenLanguage,
        account = account,
        profileLink = profileLink,
    )
}
