package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.core.support.domain.usecase.impl.ObserveSupportDiagnosticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ObserveSupportDiagnosticsUseCaseTest {
    private lateinit var language: MutableStateFlow<AppLanguage>
    private lateinit var status: MutableStateFlow<AccountStatus>
    private lateinit var profileLink: MutableStateFlow<String?>
    private lateinit var useCase: ObserveSupportDiagnosticsUseCase
    private val emitted = mutableListOf<SupportDiagnostics>()

    @Test
    fun `GIVEN a local only install WHEN observing the diagnostics THEN the account is local only`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportAccount.LOCAL_ONLY, diagnostics.account)
    }

    @Test
    fun `GIVEN an anonymous account WHEN observing the diagnostics THEN the account is anonymous`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Anonymous(USER_ID))

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportAccount.ANONYMOUS, diagnostics.account)
    }

    @Test
    fun `GIVEN a connected Google account WHEN observing the diagnostics THEN the account is Google`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE))

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportAccount.GOOGLE, diagnostics.account)
    }

    @Test
    fun `GIVEN a connected Apple account WHEN observing the diagnostics THEN the account is Apple`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Connected(USER_ID, AccountProvider.APPLE))

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportAccount.APPLE, diagnostics.account)
    }

    @Test
    fun `GIVEN an anonymous account WHEN observing the diagnostics THEN the user id is nowhere in them`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Anonymous(USER_ID))

        // When
        val diagnostics = useCase().first()

        // Then
        assertFalse(diagnostics.toString().contains(USER_ID))
    }

    @Test
    fun `GIVEN a connected account WHEN observing the diagnostics THEN the user id is nowhere in them`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE))

        // When
        val diagnostics = useCase().first()

        // Then
        assertFalse(diagnostics.toString().contains(USER_ID))
    }

    @Test
    fun `GIVEN the version the app was built with WHEN observing the diagnostics THEN they carry it`() = runTest {
        // Given
        prepareScenario(appVersion = "2.7.1")

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals("2.7.1", diagnostics.appVersion)
    }

    @Test
    fun `GIVEN the Android app WHEN observing the diagnostics THEN the platform is Android`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.ANDROID)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportPlatform.ANDROID, diagnostics.platform)
    }

    @Test
    fun `GIVEN the iOS app WHEN observing the diagnostics THEN the platform is iOS`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.IOS)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportPlatform.IOS, diagnostics.platform)
    }

    @Test
    fun `GIVEN the Mac app WHEN observing the diagnostics THEN the platform is macOS`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.MACOS)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportPlatform.MACOS, diagnostics.platform)
    }

    @Test
    fun `GIVEN the web app WHEN observing the diagnostics THEN the platform is Web`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.WEB)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(SupportPlatform.WEB, diagnostics.platform)
    }

    @Test
    fun `GIVEN the app in Portuguese WHEN observing the diagnostics THEN the language is Portuguese`() = runTest {
        // Given
        prepareScenario(appLanguage = AppLanguage.PORTUGUESE)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(AppLanguage.PORTUGUESE, diagnostics.language)
    }

    @Test
    fun `GIVEN the app follows the device WHEN observing the diagnostics THEN the language is the system one`() =
        runTest {
            // Given
            prepareScenario(appLanguage = AppLanguage.SYSTEM)

            // When
            val diagnostics = useCase().first()

            // Then
            assertEquals(AppLanguage.SYSTEM, diagnostics.language)
        }

    @Test
    fun `GIVEN the profile has its link WHEN observing the diagnostics THEN the link passes through as is`() = runTest {
        // Given
        prepareScenario(link = PROFILE_LINK)

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(PROFILE_LINK, diagnostics.profileLink)
    }

    @Test
    fun `GIVEN the profile has no code yet WHEN observing the diagnostics THEN there is no profile link`() = runTest {
        // Given
        prepareScenario(link = null)

        // When
        val diagnostics = useCase().first()

        // Then
        assertNull(diagnostics.profileLink)
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN nothing changes THEN they are emitted once`() = runTest {
        // Given
        prepareScenario()

        // When
        collectDiagnostics()

        // Then
        assertEquals(
            listOf(
                SupportDiagnostics(
                    appVersion = "1.4.0",
                    platform = SupportPlatform.ANDROID,
                    language = AppLanguage.SYSTEM,
                    spokenLanguage = "en",
                    account = SupportAccount.LOCAL_ONLY,
                    profileLink = null,
                ),
            ),
            emitted,
        )
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN the language changes THEN a new value carries it`() = runTest {
        // Given
        prepareScenario(appLanguage = AppLanguage.SYSTEM)
        collectDiagnostics()

        // When
        language.value = AppLanguage.JAPANESE
        runCurrent()

        // Then
        assertEquals(listOf(AppLanguage.SYSTEM, AppLanguage.JAPANESE), emitted.map { it.language })
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN the install registers THEN a new value is anonymous`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)
        collectDiagnostics()

        // When
        status.value = AccountStatus.Anonymous(USER_ID)
        runCurrent()

        // Then
        assertEquals(listOf(SupportAccount.LOCAL_ONLY, SupportAccount.ANONYMOUS), emitted.map { it.account })
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN the account connects to Apple THEN a new value is Apple`() = runTest {
        // Given
        prepareScenario(accountStatus = AccountStatus.Anonymous(USER_ID))
        collectDiagnostics()

        // When
        status.value = AccountStatus.Connected(USER_ID, AccountProvider.APPLE)
        runCurrent()

        // Then
        assertEquals(listOf(SupportAccount.ANONYMOUS, SupportAccount.APPLE), emitted.map { it.account })
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN the profile gets its link THEN a new value carries it`() = runTest {
        // Given
        prepareScenario(link = null)
        collectDiagnostics()

        // When
        profileLink.value = PROFILE_LINK
        runCurrent()

        // Then
        assertEquals(listOf(null, PROFILE_LINK), emitted.map { it.profileLink })
    }

    @Test
    fun `GIVEN the diagnostics are observed WHEN the profile loses its link THEN a new value has none`() = runTest {
        // Given
        prepareScenario(link = PROFILE_LINK)
        collectDiagnostics()

        // When
        profileLink.value = null
        runCurrent()

        // Then
        assertEquals(listOf(PROFILE_LINK, null), emitted.map { it.profileLink })
    }

    private fun TestScope.collectDiagnostics() {
        backgroundScope.launch { useCase().collect(emitted::add) }
        runCurrent()
    }

    @Test
    fun `GIVEN the app follows a device in Portuguese WHEN observing THEN names the language it speaks`() = runTest {
        // Given
        prepareScenario(
            appLanguage = AppLanguage.SYSTEM,
            spokenLanguage = "pt",
        )

        // When
        val diagnostics = useCase().first()

        // Then
        assertEquals(AppLanguage.SYSTEM, diagnostics.language)
        assertEquals("pt", diagnostics.spokenLanguage)
    }

    private fun prepareScenario(
        appVersion: String = "1.4.0",
        platform: ClientPlatform = ClientPlatform.ANDROID,
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        accountStatus: AccountStatus = AccountStatus.LocalOnly,
        link: String? = null,
        spokenLanguage: String = "en",
    ) {
        language = MutableStateFlow(appLanguage)
        status = MutableStateFlow(accountStatus)
        profileLink = MutableStateFlow(link)
        useCase = ObserveSupportDiagnosticsUseCase(
            appVersion = appVersion,
            platform = platform,
            observeAppLanguage = { language },
            observeSpokenLanguage = { flowOf(spokenLanguage) },
            observeAccountStatus = { status },
            observeProfileShareLink = { profileLink },
        )
    }

    private companion object {
        const val USER_ID = "3f2b8c1e-user-id"
        const val PROFILE_LINK = "https://blitzsplit.com/u/7Hk2pQ8xZa"
    }
}
