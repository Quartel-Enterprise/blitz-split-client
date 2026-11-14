package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
import software.quare.loadable.toLoaded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileUiStateTest {
    // Why: what an English UI hands for each option: its own name, then its name in English.
    private val englishNames: (AppLanguage) -> List<String> = { language ->
        when (language) {
            AppLanguage.SYSTEM -> listOf("Device language")
            AppLanguage.ENGLISH -> listOf("English", "English")
            AppLanguage.PORTUGUESE -> listOf("Português (Brasil)", "Portuguese (Brazil)")
            AppLanguage.SPANISH -> listOf("Español", "Spanish")
            AppLanguage.FRENCH -> listOf("Français", "French")
            AppLanguage.GERMAN -> listOf("Deutsch", "German")
            AppLanguage.ITALIAN -> listOf("Italiano", "Italian")
            AppLanguage.POLISH -> listOf("Polski", "Polish")
            AppLanguage.DUTCH -> listOf("Nederlands", "Dutch")
            AppLanguage.JAPANESE -> listOf("日本語", "Japanese")
            AppLanguage.TURKISH -> listOf("Türkçe", "Turkish")
            AppLanguage.INDONESIAN -> listOf("Bahasa Indonesia", "Indonesian")
        }
    }

    // Why: what a Portuguese UI hands: German is "Alemão" there.
    private val portugueseNames: (AppLanguage) -> List<String> = { language ->
        when (language) {
            AppLanguage.SYSTEM -> listOf("Idioma do aparelho")
            AppLanguage.GERMAN -> listOf("Deutsch", "Alemão")
            else -> englishNames(language).take(1)
        }
    }

    private val allOptions = listOf(
        AppLanguage.SYSTEM,
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

    @Test
    fun `GIVEN a name in lower case with spaces around it WHEN drawing the avatar THEN shows its upper case initial`() {
        // Given
        val profile = state(displayName = "  pierre  ")

        // When
        val initial = profile.avatarInitial

        // Then
        assertEquals("P", initial)
    }

    @Test
    fun `GIVEN a blank name WHEN drawing the avatar THEN shows no initial`() {
        // Given
        val profile = state(displayName = "   ")

        // When
        val initial = profile.avatarInitial

        // Then
        assertEquals("", initial)
    }

    @Test
    fun `GIVEN online and nothing under way WHEN drawing the account buttons THEN enables them`() {
        // Given
        val profile = state()

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertTrue(isEnabled)
    }

    @Test
    fun `GIVEN no internet WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN Google connecting WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(connectingProvider = AccountProvider.GOOGLE)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN the code kept ticked WHEN drawing the erase button THEN enables it`() {
        // Given
        val profile = state(hasConfirmedCodeSaved = true)

        // When
        val canErase = profile.canErase

        // Then
        assertTrue(canErase)
    }

    @Test
    fun `GIVEN the code kept ticked and the erase under way WHEN drawing the erase button THEN disables it`() {
        // Given
        val profile = state(
            hasConfirmedCodeSaved = true,
            isSigningOut = true,
        )

        // When
        val canErase = profile.canErase

        // Then
        assertFalse(canErase)
    }

    @Test
    fun `GIVEN the code kept not ticked WHEN drawing the erase button THEN disables it`() {
        // Given
        val profile = state(hasConfirmedCodeSaved = false)

        // When
        val canErase = profile.canErase

        // Then
        assertFalse(canErase)
    }

    @Test
    fun `GIVEN a profile change not uploaded and no sync status WHEN drawing the profile THEN shows it pending`() {
        // Given
        val profile = state(
            pendingChanges = listOf(PendingChange.PROFILE),
            syncStatus = null,
        )

        // When
        val showsPending = profile.showsProfilePending

        // Then
        assertTrue(showsPending)
    }

    @Test
    fun `GIVEN online and nothing under way WHEN drawing the connect button THEN enables it`() {
        // Given
        val profile = state()

        // When
        val canConnect = profile.canConnect

        // Then
        assertTrue(canConnect)
    }

    @Test
    fun `GIVEN no internet WHEN drawing the connect button THEN disables it`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val canConnect = profile.canConnect

        // Then
        assertFalse(canConnect)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the connect button THEN disables it`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val canConnect = profile.canConnect

        // Then
        assertFalse(canConnect)
    }

    @Test
    fun `GIVEN no account and connect expanded WHEN drawing the account row THEN shows the provider buttons`() {
        // Given
        val profile = state(
            accountProvider = null,
            isConnectExpanded = true,
        )

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertTrue(showsButtons)
    }

    @Test
    fun `GIVEN no account and connect not expanded WHEN drawing the account row THEN hides the provider buttons`() {
        // Given
        val profile = state(
            accountProvider = null,
            isConnectExpanded = false,
        )

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertFalse(showsButtons)
    }

    @Test
    fun `GIVEN Google connected and connect expanded WHEN drawing the account row THEN hides the provider buttons`() {
        // Given
        val profile = state(
            accountProvider = AccountProvider.GOOGLE,
            isConnectExpanded = true,
        )

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertFalse(showsButtons)
    }

    @Test
    fun `GIVEN nothing under way WHEN drawing the sign out button THEN enables it`() {
        // Given
        val profile = state()

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertTrue(canSignOut)
    }

    @Test
    fun `GIVEN no internet and nothing under way WHEN drawing the sign out button THEN enables it`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertTrue(canSignOut)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN a sign out scheduled once synced WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(isSignOutScheduled = true)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN Apple connecting WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(connectingProvider = AccountProvider.APPLE)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN a profile change not uploaded WHEN warning before erasing THEN says there are pending changes`() {
        // Given
        val profile = state(pendingChanges = listOf(PendingChange.PROFILE))

        // When
        val hasPending = profile.hasPendingChanges

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN nothing left to upload WHEN warning before erasing THEN says there are no pending changes`() {
        // Given
        val profile = state(pendingChanges = emptyList())

        // When
        val hasPending = profile.hasPendingChanges

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `GIVEN a recovery code WHEN drawing the profile THEN has a code to show`() {
        // Given
        val profile = state(formattedRecoveryCode = "K7QM-4XPA-9TRE")

        // When
        val hasCode = profile.hasRecoveryCode

        // Then
        assertTrue(hasCode)
    }

    @Test
    fun `GIVEN no recovery code WHEN drawing the profile THEN has no code to show`() {
        // Given
        val profile = state(formattedRecoveryCode = null)

        // When
        val hasCode = profile.hasRecoveryCode

        // Then
        assertFalse(hasCode)
    }

    @Test
    fun `GIVEN no account and a recovery code WHEN drawing the account section THEN shows the recovery code row`() {
        // Given
        val profile = state(
            accountProvider = null,
            formattedRecoveryCode = "K7QM-4XPA-9TRE",
        )

        // When
        val showsRow = profile.showsRecoveryCodeRow

        // Then
        assertTrue(showsRow)
    }

    @Test
    fun `GIVEN no account and no recovery code WHEN drawing the account section THEN hides the recovery code row`() {
        // Given
        val profile = state(
            accountProvider = null,
            formattedRecoveryCode = null,
        )

        // When
        val showsRow = profile.showsRecoveryCodeRow

        // Then
        assertFalse(showsRow)
    }

    @Test
    fun `GIVEN Apple and a recovery code WHEN drawing the account section THEN hides the recovery code row`() {
        // Given
        val profile = state(
            accountProvider = AccountProvider.APPLE,
            formattedRecoveryCode = "K7QM-4XPA-9TRE",
        )

        // When
        val showsRow = profile.showsRecoveryCodeRow

        // Then
        assertFalse(showsRow)
    }

    @Test
    fun `GIVEN Google WHEN drawing the danger zone THEN shows delete account`() {
        // Given
        val profile = state(accountProvider = AccountProvider.GOOGLE)

        // When
        val showsDelete = profile.showsDeleteAccount

        // Then
        assertTrue(showsDelete)
    }

    @Test
    fun `GIVEN Apple WHEN drawing the danger zone THEN shows delete account`() {
        // Given
        val profile = state(accountProvider = AccountProvider.APPLE)

        // When
        val showsDelete = profile.showsDeleteAccount

        // Then
        assertTrue(showsDelete)
    }

    @Test
    fun `GIVEN only a local profile WHEN drawing the danger zone THEN hides delete account`() {
        // Given
        val profile = state(accountProvider = null)

        // When
        val showsDelete = profile.showsDeleteAccount

        // Then
        assertFalse(showsDelete)
    }

    @Test
    fun `GIVEN an anonymous account WHEN drawing the danger zone THEN shows delete account`() {
        // Given
        val profile = state(
            accountProvider = null,
            hasAnonymousId = true,
        )

        // When
        val showsDelete = profile.showsDeleteAccount

        // Then
        assertTrue(showsDelete)
    }

    @Test
    fun `GIVEN an anonymous account not read yet WHEN drawing the danger zone THEN hides delete account`() {
        // Given
        val profile = state(
            isLoaded = false,
            accountProvider = null,
            hasAnonymousId = true,
        )

        // When
        val showsDelete = profile.showsDeleteAccount

        // Then
        assertFalse(showsDelete)
    }

    @Test
    fun `GIVEN an anonymous account with its recovery code WHEN drawing the sign out THEN signs out without erasing`() {
        // Given
        val profile = state(
            hasAnonymousId = true,
            formattedRecoveryCode = "ABCD-EFGH-JKLM",
        )

        // When
        val erases = profile.erasesProfileOnSignOut

        // Then
        assertFalse(erases)
    }

    @Test
    fun `GIVEN an anonymous account whose code is not made yet WHEN drawing the sign out THEN erases the profile`() {
        // Given
        val profile = state(
            hasAnonymousId = true,
            formattedRecoveryCode = null,
        )

        // When
        val erases = profile.erasesProfileOnSignOut

        // Then
        assertTrue(erases)
    }

    @Test
    fun `GIVEN Google without a recovery code WHEN drawing the sign out THEN signs out without erasing`() {
        // Given
        val profile = state(
            accountProvider = AccountProvider.GOOGLE,
            formattedRecoveryCode = null,
        )

        // When
        val erases = profile.erasesProfileOnSignOut

        // Then
        assertFalse(erases)
    }

    @Test
    fun `GIVEN no account nor anonymous id WHEN drawing the sign out button THEN treats it as local only`() {
        // Given
        val profile = state(
            accountProvider = null,
            hasAnonymousId = false,
        )

        // When
        val isLocalOnly = profile.isLocalOnly

        // Then
        assertTrue(isLocalOnly)
    }

    @Test
    fun `GIVEN an anonymous account WHEN drawing the sign out button THEN does not treat the profile as local only`() {
        // Given
        val profile = state(
            accountProvider = null,
            hasAnonymousId = true,
        )

        // When
        val isLocalOnly = profile.isLocalOnly

        // Then
        assertFalse(isLocalOnly)
    }

    @Test
    fun `GIVEN Google WHEN drawing the sign out button THEN does not treat the profile as local only`() {
        // Given
        val profile = state(accountProvider = AccountProvider.GOOGLE)

        // When
        val isLocalOnly = profile.isLocalOnly

        // Then
        assertFalse(isLocalOnly)
    }

    @Test
    fun `GIVEN Apple WHEN drawing the sign out button THEN does not treat the profile as local only`() {
        // Given
        val profile = state(accountProvider = AccountProvider.APPLE)

        // When
        val isLocalOnly = profile.isLocalOnly

        // Then
        assertFalse(isLocalOnly)
    }

    @Test
    fun `GIVEN the profile not read yet WHEN drawing it THEN shows neither Excluir conta nor the recovery code`() {
        // Given
        val profile = state(
            isLoaded = false,
            accountProvider = AccountProvider.GOOGLE,
            formattedRecoveryCode = "K7QM-4XPA-9TRE",
        )

        // When
        val showsDelete = profile.showsDeleteAccount
        val showsCode = profile.showsRecoveryCodeRow

        // Then
        assertFalse(showsDelete)
        assertFalse(showsCode)
    }

    @Test
    fun `GIVEN the system language WHEN listing the languages THEN offers System then each language`() {
        // Given
        val profile = state(language = AppLanguage.SYSTEM)

        // When
        val options = profile.languageOptions

        // Then
        assertEquals(
            listOf(
                AppLanguage.SYSTEM,
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
            ),
            options,
        )
    }

    @Test
    fun `GIVEN Portuguese picked WHEN listing the languages THEN still offers System first and every language`() {
        // Given
        val profile = state(language = AppLanguage.PORTUGUESE)

        // When
        val options = profile.languageOptions

        // Then
        assertEquals(
            listOf(
                AppLanguage.SYSTEM,
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
            ),
            options,
        )
    }

    @Test
    fun `GIVEN no search typed WHEN searching the languages THEN offers every option in order`() {
        // Given
        val profile = state(languageQuery = "")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(allOptions, options)
    }

    @Test
    fun `GIVEN only spaces typed WHEN searching the languages THEN offers every option in order`() {
        // Given
        val profile = state(languageQuery = "   ")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(allOptions, options)
    }

    @Test
    fun `GIVEN portugues typed without its accent WHEN searching the languages THEN finds Portuguese`() {
        // Given
        val profile = state(languageQuery = "portugues")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), options)
    }

    @Test
    fun `GIVEN BRASIL typed in upper case WHEN searching the languages THEN finds Portuguese`() {
        // Given
        val profile = state(languageQuery = "BRASIL")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), options)
    }

    @Test
    fun `GIVEN the name in the app language typed WHEN searching the languages THEN finds that language`() {
        // Given
        val profile = state(languageQuery = "dutch")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.DUTCH), options)
    }

    @Test
    fun `GIVEN alemao typed in a Portuguese app WHEN searching the languages THEN finds German`() {
        // Given
        val profile = state(languageQuery = "alemao")

        // When
        val options = profile.findLanguageOptions(portugueseNames)

        // Then
        assertEquals(listOf(AppLanguage.GERMAN), options)
    }

    @Test
    fun `GIVEN the code pt typed and no names WHEN searching the languages THEN finds Portuguese by its code`() {
        // Given
        val profile = state(languageQuery = "pt")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), options)
    }

    @Test
    fun `GIVEN the code NL typed in upper case WHEN searching the languages THEN finds Dutch by its code`() {
        // Given
        val profile = state(languageQuery = "NL")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.DUTCH), options)
    }

    @Test
    fun `GIVEN en typed WHEN searching the languages THEN finds English by its code and French by its name`() {
        // Given
        val profile = state(languageQuery = "en")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.ENGLISH, AppLanguage.FRENCH), options)
    }

    @Test
    fun `GIVEN device typed WHEN searching the languages THEN finds the device language option`() {
        // Given
        val profile = state(languageQuery = "device")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.SYSTEM), options)
    }

    @Test
    fun `GIVEN a name with spaces around it WHEN searching the languages THEN finds that language`() {
        // Given
        val profile = state(languageQuery = "  french  ")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.FRENCH), options)
    }

    @Test
    fun `GIVEN ish typed WHEN searching the languages THEN finds every match in the list order`() {
        // Given
        val profile = state(languageQuery = "ish")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.POLISH, AppLanguage.TURKISH), options)
    }

    @Test
    fun `GIVEN a name no language has WHEN searching the languages THEN finds nothing`() {
        // Given
        val profile = state(languageQuery = "klingon")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(emptyList<AppLanguage>(), options)
    }

    @Test
    fun `GIVEN the region br of pt-BR typed and no names WHEN searching the languages THEN finds nothing by code`() {
        // Given
        val profile = state(languageQuery = "br")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(emptyList<AppLanguage>(), options)
    }

    private fun state(
        isLoaded: Boolean = true,
        displayName: String = "Pierre",
        isOnline: Boolean = true,
        syncStatus: ProfileSyncStatus? = null,
        pendingChanges: List<PendingChange> = emptyList(),
        hasConfirmedCodeSaved: Boolean = false,
        connectingProvider: AccountProvider? = null,
        isSigningOut: Boolean = false,
        isSignOutScheduled: Boolean = false,
        accountProvider: AccountProvider? = null,
        hasAnonymousId: Boolean = false,
        isConnectExpanded: Boolean = false,
        formattedRecoveryCode: String? = null,
        language: AppLanguage = AppLanguage.SYSTEM,
        languageQuery: String = "",
    ): ProfileUiState = ProfileUiState(
        isLoaded = isLoaded,
        displayName = displayName,
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        shareLink = null,
        accountProvider = accountProvider,
        hasAnonymousId = hasAnonymousId,
        email = null,
        formattedRecoveryCode = formattedRecoveryCode,
        showsRecoveryCodePrompt = false,
        isOnline = isOnline,
        syncStatus = syncStatus,
        pendingChanges = pendingChanges,
        paymentKeys = emptyList<PaymentKeyItem>().toLoaded(),
        showsPix = true,
        isSignOutScheduled = isSignOutScheduled,
        theme = ThemePreference.SYSTEM,
        showsDynamicColors = true,
        usesDynamicColors = false,
        showsBitcoinInSats = false,
        sharesUsageData = true,
        language = language,
        languageQuery = languageQuery,
        activeNotificationCount = 3.toLoaded(),
        showsCheckForUpdates = true,
        isCheckingForUpdates = false,
        dialog = null,
        hasConfirmedCodeSaved = hasConfirmedCodeSaved,
        isConnectExpanded = isConnectExpanded,
        connectingProvider = connectingProvider,
        hasSignInFailed = false,
        isSigningOut = isSigningOut,
        hasSignOutFailed = false,
        appInvite = null,
    )
}
