package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.TimeFormatPreference
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
            AppLanguage.PORTUGUESE_PORTUGAL -> listOf("Português (Portugal)", "Portuguese (Portugal)")
            AppLanguage.SPANISH -> listOf("Español (Latinoamérica)", "Spanish (Latin America)")
            AppLanguage.SPANISH_SPAIN -> listOf("Español (España)", "Spanish (Spain)")
            AppLanguage.FRENCH -> listOf("Français", "French")
            AppLanguage.GERMAN -> listOf("Deutsch", "German")
            AppLanguage.ITALIAN -> listOf("Italiano", "Italian")
            AppLanguage.POLISH -> listOf("Polski", "Polish")
            AppLanguage.DUTCH -> listOf("Nederlands", "Dutch")
            AppLanguage.JAPANESE -> listOf("日本語", "Japanese")
            AppLanguage.TURKISH -> listOf("Türkçe", "Turkish")
            AppLanguage.INDONESIAN -> listOf("Bahasa Indonesia", "Indonesian")
            AppLanguage.RUSSIAN -> listOf("Русский", "Russian")
            AppLanguage.UKRAINIAN -> listOf("Українська", "Ukrainian")
            AppLanguage.KOREAN -> listOf("한국어", "Korean")
            AppLanguage.HINDI -> listOf("हिन्दी", "Hindi")
            AppLanguage.ARABIC -> listOf("العربية", "Arabic")
            AppLanguage.CHINESE_SIMPLIFIED -> listOf("中文（简体）", "Chinese (Simplified)")
            AppLanguage.HEBREW -> listOf("עברית", "Hebrew")
            AppLanguage.THAI -> listOf("ไทย", "Thai")
            AppLanguage.VIETNAMESE -> listOf("Tiếng Việt", "Vietnamese")
            AppLanguage.SWEDISH -> listOf("Svenska", "Swedish")
            AppLanguage.CZECH -> listOf("Čeština", "Czech")
            AppLanguage.ROMANIAN -> listOf("Română", "Romanian")
            AppLanguage.BENGALI -> listOf("বাংলা", "Bengali")
            AppLanguage.MALAY -> listOf("Bahasa Melayu", "Malay")
            AppLanguage.GREEK -> listOf("Ελληνικά", "Greek")
            AppLanguage.CROATIAN -> listOf("Hrvatski", "Croatian")
            AppLanguage.HUNGARIAN -> listOf("Magyar", "Hungarian")
            AppLanguage.URDU -> listOf("اردو", "Urdu")
            AppLanguage.FILIPINO -> listOf("Filipino", "Filipino")
            AppLanguage.SERBIAN -> listOf("Srpski", "Serbian")
            AppLanguage.SLOVAK -> listOf("Slovenčina", "Slovak")
            AppLanguage.PERSIAN -> listOf("فارسی", "Persian")
            AppLanguage.TAMIL -> listOf("தமிழ்", "Tamil")
            AppLanguage.NORWEGIAN -> listOf("Norsk bokmål", "Norwegian Bokmål")
            AppLanguage.TELUGU -> listOf("తెలుగు", "Telugu")
            AppLanguage.BULGARIAN -> listOf("Български", "Bulgarian")
            AppLanguage.MARATHI -> listOf("मराठी", "Marathi")
            AppLanguage.CATALAN -> listOf("Català", "Catalan")
            AppLanguage.CHINESE_TRADITIONAL -> listOf("中文（繁體）", "Chinese (Traditional)")
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
        AppLanguage.PORTUGUESE_PORTUGAL,
        AppLanguage.SPANISH,
        AppLanguage.SPANISH_SPAIN,
        AppLanguage.FRENCH,
        AppLanguage.GERMAN,
        AppLanguage.ITALIAN,
        AppLanguage.POLISH,
        AppLanguage.DUTCH,
        AppLanguage.JAPANESE,
        AppLanguage.TURKISH,
        AppLanguage.INDONESIAN,
        AppLanguage.RUSSIAN,
        AppLanguage.UKRAINIAN,
        AppLanguage.KOREAN,
        AppLanguage.HINDI,
        AppLanguage.ARABIC,
        AppLanguage.CHINESE_SIMPLIFIED,
        AppLanguage.CHINESE_TRADITIONAL,
        AppLanguage.HEBREW,
        AppLanguage.THAI,
        AppLanguage.VIETNAMESE,
        AppLanguage.SWEDISH,
        AppLanguage.CZECH,
        AppLanguage.ROMANIAN,
        AppLanguage.BENGALI,
        AppLanguage.MALAY,
        AppLanguage.GREEK,
        AppLanguage.CROATIAN,
        AppLanguage.HUNGARIAN,
        AppLanguage.URDU,
        AppLanguage.FILIPINO,
        AppLanguage.SERBIAN,
        AppLanguage.SLOVAK,
        AppLanguage.PERSIAN,
        AppLanguage.TAMIL,
        AppLanguage.NORWEGIAN,
        AppLanguage.TELUGU,
        AppLanguage.BULGARIAN,
        AppLanguage.MARATHI,
        AppLanguage.CATALAN,
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
        val profile = state(recoveryWords = RECOVERY_CODE.split(" "))

        // When
        val hasCode = profile.hasRecoveryCode

        // Then
        assertTrue(hasCode)
    }

    @Test
    fun `GIVEN no recovery code WHEN drawing the profile THEN has no code to show`() {
        // Given
        val profile = state(recoveryWords = null)

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
            recoveryWords = RECOVERY_CODE.split(" "),
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
            recoveryWords = null,
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
            recoveryWords = RECOVERY_CODE.split(" "),
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
            recoveryWords = RECOVERY_CODE.split(" "),
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
            recoveryWords = null,
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
            recoveryWords = null,
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
            recoveryWords = RECOVERY_CODE.split(" "),
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
                AppLanguage.PORTUGUESE_PORTUGAL,
                AppLanguage.SPANISH,
                AppLanguage.SPANISH_SPAIN,
                AppLanguage.FRENCH,
                AppLanguage.GERMAN,
                AppLanguage.ITALIAN,
                AppLanguage.POLISH,
                AppLanguage.DUTCH,
                AppLanguage.JAPANESE,
                AppLanguage.TURKISH,
                AppLanguage.INDONESIAN,
                AppLanguage.RUSSIAN,
                AppLanguage.UKRAINIAN,
                AppLanguage.KOREAN,
                AppLanguage.HINDI,
                AppLanguage.ARABIC,
                AppLanguage.CHINESE_SIMPLIFIED,
                AppLanguage.CHINESE_TRADITIONAL,
                AppLanguage.HEBREW,
                AppLanguage.THAI,
                AppLanguage.VIETNAMESE,
                AppLanguage.SWEDISH,
                AppLanguage.CZECH,
                AppLanguage.ROMANIAN,
                AppLanguage.BENGALI,
                AppLanguage.MALAY,
                AppLanguage.GREEK,
                AppLanguage.CROATIAN,
                AppLanguage.HUNGARIAN,
                AppLanguage.URDU,
                AppLanguage.FILIPINO,
                AppLanguage.SERBIAN,
                AppLanguage.SLOVAK,
                AppLanguage.PERSIAN,
                AppLanguage.TAMIL,
                AppLanguage.NORWEGIAN,
                AppLanguage.TELUGU,
                AppLanguage.BULGARIAN,
                AppLanguage.MARATHI,
                AppLanguage.CATALAN,
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
                AppLanguage.PORTUGUESE_PORTUGAL,
                AppLanguage.SPANISH,
                AppLanguage.SPANISH_SPAIN,
                AppLanguage.FRENCH,
                AppLanguage.GERMAN,
                AppLanguage.ITALIAN,
                AppLanguage.POLISH,
                AppLanguage.DUTCH,
                AppLanguage.JAPANESE,
                AppLanguage.TURKISH,
                AppLanguage.INDONESIAN,
                AppLanguage.RUSSIAN,
                AppLanguage.UKRAINIAN,
                AppLanguage.KOREAN,
                AppLanguage.HINDI,
                AppLanguage.ARABIC,
                AppLanguage.CHINESE_SIMPLIFIED,
                AppLanguage.CHINESE_TRADITIONAL,
                AppLanguage.HEBREW,
                AppLanguage.THAI,
                AppLanguage.VIETNAMESE,
                AppLanguage.SWEDISH,
                AppLanguage.CZECH,
                AppLanguage.ROMANIAN,
                AppLanguage.BENGALI,
                AppLanguage.MALAY,
                AppLanguage.GREEK,
                AppLanguage.CROATIAN,
                AppLanguage.HUNGARIAN,
                AppLanguage.URDU,
                AppLanguage.FILIPINO,
                AppLanguage.SERBIAN,
                AppLanguage.SLOVAK,
                AppLanguage.PERSIAN,
                AppLanguage.TAMIL,
                AppLanguage.NORWEGIAN,
                AppLanguage.TELUGU,
                AppLanguage.BULGARIAN,
                AppLanguage.MARATHI,
                AppLanguage.CATALAN,
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
    fun `GIVEN portugues typed without its accent WHEN searching the languages THEN finds both Portuguese`() {
        // Given
        val profile = state(languageQuery = "portugues")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE, AppLanguage.PORTUGUESE_PORTUGAL), options)
    }

    @Test
    fun `GIVEN portugal typed WHEN searching the languages THEN finds the Portuguese of Portugal only`() {
        // Given
        val profile = state(languageQuery = "portugal")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE_PORTUGAL), options)
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
    fun `GIVEN the code pt typed and no names WHEN searching the languages THEN finds both Portuguese by their code`() {
        // Given
        val profile = state(languageQuery = "pt")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE, AppLanguage.PORTUGUESE_PORTUGAL), options)
    }

    @Test
    fun `GIVEN ua or uk typed and no names WHEN searching the languages THEN finds Ukrainian by chip or code`() {
        // Given
        val queries = listOf("ua", "uk")

        // When
        val options = queries.map { state(languageQuery = it).findLanguageOptions({ emptyList() }) }

        // Then
        assertEquals(List(queries.size) { listOf(AppLanguage.UKRAINIAN) }, options)
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
    fun `GIVEN en typed WHEN searching the languages THEN finds English by its code and others by their names`() {
        // Given
        val profile = state(languageQuery = "en")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(
            listOf(
                AppLanguage.ENGLISH,
                AppLanguage.FRENCH,
                AppLanguage.SWEDISH,
                AppLanguage.BENGALI,
                AppLanguage.SLOVAK,
            ),
            options,
        )
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
        assertEquals(
            listOf(
                AppLanguage.ENGLISH,
                AppLanguage.SPANISH,
                AppLanguage.SPANISH_SPAIN,
                AppLanguage.POLISH,
                AppLanguage.TURKISH,
                AppLanguage.SWEDISH,
            ),
            options,
        )
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
    fun `GIVEN the chip code PT in upper case and no names WHEN searching the languages THEN finds both Portuguese`() {
        // Given
        val profile = state(languageQuery = "PT")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE, AppLanguage.PORTUGUESE_PORTUGAL), options)
    }

    @Test
    fun `GIVEN the code es typed and no names WHEN searching the languages THEN finds both Spanish by their code`() {
        // Given
        val profile = state(languageQuery = "es")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.SPANISH, AppLanguage.SPANISH_SPAIN), options)
    }

    @Test
    fun `GIVEN the chip code 419 and no names WHEN searching the languages THEN finds the Latin American Spanish`() {
        // Given
        val profile = state(languageQuery = "419")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.SPANISH), options)
    }

    @Test
    fun `GIVEN espana typed without its tilde WHEN searching the languages THEN finds the Spanish of Spain only`() {
        // Given
        val profile = state(languageQuery = "espana")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.SPANISH_SPAIN), options)
    }

    @Test
    fun `GIVEN brazil typed WHEN searching the languages THEN finds the Portuguese of Brazil only`() {
        // Given
        val profile = state(languageQuery = "brazil")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), options)
    }

    @Test
    fun `GIVEN the chip code br typed WHEN searching the languages THEN does not find the Portuguese of Portugal`() {
        // Given
        val profile = state(languageQuery = "br")

        // When
        val options = profile.findLanguageOptions(englishNames)

        // Then
        assertFalse(AppLanguage.PORTUGUESE_PORTUGAL in options)
    }

    @Test
    fun `GIVEN the chip code br of Brazil typed and no names WHEN searching the languages THEN finds Portuguese`() {
        // Given
        val profile = state(languageQuery = "br")

        // When
        val options = profile.findLanguageOptions({ emptyList() })

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), options)
    }

    @Test
    fun `GIVEN a loaded profile with a server user id WHEN drawing Account and sync THEN shows the User ID row`() {
        // Given
        val profile = state(
            hasAnonymousId = true,
            userId = USER_ID,
        )

        // When
        val showsUserId = profile.showsUserId

        // Then
        assertTrue(showsUserId)
    }

    @Test
    fun `GIVEN a loaded local only profile WHEN drawing Account and sync THEN hides the User ID row`() {
        // Given
        val profile = state(userId = null)

        // When
        val showsUserId = profile.showsUserId

        // Then
        assertFalse(showsUserId)
    }

    @Test
    fun `GIVEN a profile with a user id not loaded yet WHEN drawing Account and sync THEN hides the User ID row`() {
        // Given
        val profile = state(
            isLoaded = false,
            userId = USER_ID,
        )

        // When
        val showsUserId = profile.showsUserId

        // Then
        assertFalse(showsUserId)
    }

    @Test
    fun `GIVEN a server user id WHEN drawing the User ID row THEN shows its start and end`() {
        // Given
        val profile = state(
            accountProvider = AccountProvider.GOOGLE,
            userId = USER_ID,
        )

        // When
        val shortUserId = profile.shortUserId

        // Then
        assertEquals("3f8c2a91…4f27", shortUserId)
    }

    @Test
    fun `GIVEN a local only profile WHEN drawing the User ID row THEN its short id is empty`() {
        // Given
        val profile = state(userId = null)

        // When
        val shortUserId = profile.shortUserId

        // Then
        assertEquals("", shortUserId)
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
        userId: String? = null,
        isConnectExpanded: Boolean = false,
        recoveryWords: List<String>? = null,
        language: AppLanguage = AppLanguage.SYSTEM,
        languageQuery: String = "",
    ): ProfileUiState = ProfileUiState(
        isLoaded = isLoaded,
        displayName = displayName,
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        shareLink = null,
        accountProvider = accountProvider,
        // Why: an anonymous account is one whose server id has no Google or Apple, so the flag gives it an id.
        userId = userId ?: ANONYMOUS_USER_ID.takeIf { hasAnonymousId },
        email = null,
        recoveryWords = recoveryWords,
        showsRecoveryCodePrompt = false,
        isOnline = isOnline,
        syncStatus = syncStatus,
        pendingChanges = pendingChanges,
        paymentKeys = emptyList<PaymentKeyItem>().toLoaded(),
        payingMethods = emptyList<PayingMethod>().toLoaded(),
        showsPix = true,
        isSignOutScheduled = isSignOutScheduled,
        theme = ThemePreference.SYSTEM,
        showsDynamicColors = true,
        usesDynamicColors = false,
        showsBitcoinInSats = false,
        sharesUsageData = true,
        language = language,
        languageQuery = languageQuery,
        timeFormat = TimeFormatPreference.SYSTEM,
        activeNotificationCount = 3.toLoaded(),
        showsCheckForUpdates = true,
        showsRateApp = false,
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

    private companion object {
        private const val RECOVERY_CODE = "legal winner thank year wave sausage worth useful legal winner thank yellow"
        private const val USER_ID = "3f8c2a91-6b4e-4d2a-9c7f-1e5b8a0d4f27"
        private const val ANONYMOUS_USER_ID = "anonymous-user-id"
    }
}
