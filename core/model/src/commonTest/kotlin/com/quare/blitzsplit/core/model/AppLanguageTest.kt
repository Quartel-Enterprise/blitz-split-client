package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun `GIVEN a Portuguese tag of Brazil or with no region WHEN reading it THEN is the Portuguese of Brazil`() {
        // Given
        val tags = listOf("pt", "pt-BR", "pt_BR", "PT-br", "pt-AO", "pt-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE }, languages)
    }

    @Test
    fun `GIVEN a Portuguese tag of Portugal WHEN reading it THEN is the Portuguese of Portugal`() {
        // Given
        val tags = listOf("pt-PT", "pt_PT", "PT-pt", "pt-PT-u-fw-mon", "pt-Latn-PT")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE_PORTUGAL }, languages)
    }

    @Test
    fun `GIVEN tags with and without a region WHEN reading their region THEN it is the upper case code or null`() {
        // Given
        val tags = listOf("pt-PT", "pt_br", "zh-Hans-CN", "pt-u-fw-mon", "pt", "es-419", null)

        // When
        val regions = tags.map(AppLanguage::regionOf)

        // Then
        assertEquals(listOf("PT", "BR", "CN", null, null, null, null), regions)
    }

    @Test
    fun `GIVEN the two Portuguese and the two Spanish WHEN drawing the picker chips THEN each shows its region`() {
        // Given
        val languages = listOf(
            AppLanguage.SYSTEM,
            AppLanguage.ENGLISH,
            AppLanguage.PORTUGUESE,
            AppLanguage.PORTUGUESE_PORTUGAL,
            AppLanguage.SPANISH,
            AppLanguage.SPANISH_SPAIN,
        )

        // When
        val codes = languages.map { it.chipCode }

        // Then
        assertEquals(listOf(null, "EN", "BR", "PT", "419", "ES"), codes)
    }

    @Test
    fun `GIVEN tags of languages the app has or lacks WHEN reading their language THEN it is the lower case code`() {
        // Given
        val tags = listOf("pt-BR", "PT_br", "zh-Hans-CN", "in-ID", "fi", null)

        // When
        val languages = tags.map(AppLanguage::languageOf)

        // Then
        assertEquals(listOf("pt", "pt", "zh", "id", "fi", null), languages)
    }

    @Test
    fun `GIVEN an English tag WHEN reading it THEN is English`() {
        // Given
        val tag = "en-GB"

        // When
        val language = AppLanguage.fromTag(tag)

        // Then
        assertEquals(AppLanguage.ENGLISH, language)
    }

    @Test
    fun `GIVEN no tag or a language the app does not have WHEN reading it THEN follows the device`() {
        // Given
        val tags = listOf(null, "", "fi", "fi-FI")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SYSTEM }, languages)
    }

    @Test
    fun `GIVEN a Spanish tag not of Spain WHEN reading it THEN is the Latin American Spanish`() {
        // Given
        val tags = listOf("es", "es-MX", "es_AR", "ES-us", "es-419", "es-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SPANISH }, languages)
    }

    @Test
    fun `GIVEN a Spanish tag of Spain WHEN reading it THEN is the Spanish of Spain`() {
        // Given
        val tags = listOf("es-ES", "es_ES", "ES-es", "es-ES-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SPANISH_SPAIN }, languages)
    }

    @Test
    fun `GIVEN any French tag WHEN reading it THEN is French`() {
        // Given
        val tags = listOf("fr", "fr-FR", "fr-CA", "fr_BE", "FR-ch")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.FRENCH }, languages)
    }

    @Test
    fun `GIVEN any German tag WHEN reading it THEN is German`() {
        // Given
        val tags = listOf("de", "de-DE", "de-AT", "de_CH", "DE-li")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.GERMAN }, languages)
    }

    @Test
    fun `GIVEN any Italian tag WHEN reading it THEN is Italian`() {
        // Given
        val tags = listOf("it", "it-IT", "it-CH", "it_SM", "IT-va")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.ITALIAN }, languages)
    }

    @Test
    fun `GIVEN any Polish tag WHEN reading it THEN is Polish`() {
        // Given
        val tags = listOf("pl", "pl-PL", "pl_PL", "PL-pl")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.POLISH }, languages)
    }

    @Test
    fun `GIVEN any Dutch tag WHEN reading it THEN is Dutch`() {
        // Given
        val tags = listOf("nl", "nl-NL", "nl-BE", "nl_SR", "NL-be")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.DUTCH }, languages)
    }

    @Test
    fun `GIVEN any Japanese tag WHEN reading it THEN is Japanese`() {
        // Given
        val tags = listOf("ja", "ja-JP", "ja_JP", "JA-jp")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.JAPANESE }, languages)
    }

    @Test
    fun `GIVEN any Turkish tag WHEN reading it THEN is Turkish`() {
        // Given
        val tags = listOf("tr", "tr-TR", "tr_CY", "TR-tr")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.TURKISH }, languages)
    }

    @Test
    fun `GIVEN any Indonesian tag including the legacy in WHEN reading it THEN is Indonesian`() {
        // Given
        val tags = listOf("id", "id-ID", "in", "in_ID", "IN-id")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.INDONESIAN }, languages)
    }

    @Test
    fun `GIVEN any Russian tag WHEN reading it THEN is Russian`() {
        // Given
        val tags = listOf("ru", "ru-RU", "ru_KZ", "RU-ua")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.RUSSIAN }, languages)
    }

    @Test
    fun `GIVEN any Ukrainian tag WHEN reading it THEN is Ukrainian`() {
        // Given
        val tags = listOf("uk", "uk-UA", "uk_UA", "UK-ua")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.UKRAINIAN }, languages)
    }

    @Test
    fun `GIVEN any Korean tag WHEN reading it THEN is Korean`() {
        // Given
        val tags = listOf("ko", "ko-KR", "ko_KR", "KO-kr", "ko-Kore-KR")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.KOREAN }, languages)
    }

    @Test
    fun `GIVEN any Hindi tag WHEN reading it THEN is Hindi`() {
        // Given
        val tags = listOf("hi", "hi-IN", "hi_IN", "HI-in", "hi-Deva-IN")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.HINDI }, languages)
    }

    @Test
    fun `GIVEN any Arabic tag of any country WHEN reading it THEN is the one Arabic`() {
        // Given
        val tags = listOf("ar", "ar-SA", "ar_AE", "AR-eg", "ar-MA", "ar-Arab-EG", "ar-001")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.ARABIC }, languages)
    }

    @Test
    fun `GIVEN any Chinese tag WHEN reading it THEN is Simplified Chinese`() {
        // Given
        val tags = listOf("zh", "zh-Hans", "zh-CN", "zh_CN", "ZH-hans-cn", "zh-SG", "zh-Hans-HK", "zh-CN-u-nu-hant")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.CHINESE_SIMPLIFIED }, languages)
    }

    @Test
    fun `GIVEN any Thai tag WHEN reading it THEN is Thai`() {
        // Given
        val tags = listOf("th", "th-TH", "th_TH", "TH-th", "th-TH-u-ca-buddhist")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.THAI }, languages)
    }

    @Test
    fun `GIVEN any Vietnamese tag WHEN reading it THEN is Vietnamese`() {
        // Given
        val tags = listOf("vi", "vi-VN", "vi_VN", "VI-vn")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.VIETNAMESE }, languages)
    }

    @Test
    fun `GIVEN any Swedish tag WHEN reading it THEN is Swedish`() {
        // Given
        val tags = listOf("sv", "sv-SE", "sv_FI", "SV-se")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SWEDISH }, languages)
    }

    @Test
    fun `GIVEN any Czech tag WHEN reading it THEN is Czech`() {
        // Given
        val tags = listOf("cs", "cs-CZ", "cs_CZ", "CS-cz")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.CZECH }, languages)
    }

    @Test
    fun `GIVEN any Romanian tag WHEN reading it THEN is Romanian`() {
        // Given
        val tags = listOf("ro", "ro-RO", "ro_MD", "RO-ro")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.ROMANIAN }, languages)
    }

    @Test
    fun `GIVEN any Bengali tag WHEN reading it THEN is Bengali`() {
        // Given
        val tags = listOf("bn", "bn-BD", "bn_IN", "BN-bd")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.BENGALI }, languages)
    }

    @Test
    fun `GIVEN any Malay tag WHEN reading it THEN is Malay`() {
        // Given
        val tags = listOf("ms", "ms-MY", "ms_SG", "MS-my")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.MALAY }, languages)
    }

    @Test
    fun `GIVEN any Greek tag WHEN reading it THEN is Greek`() {
        // Given
        val tags = listOf("el", "el-GR", "el_CY", "EL-gr")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.GREEK }, languages)
    }

    @Test
    fun `GIVEN any Croatian tag WHEN reading it THEN is Croatian`() {
        // Given
        val tags = listOf("hr", "hr-HR", "hr_BA", "HR-hr")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.CROATIAN }, languages)
    }

    @Test
    fun `GIVEN any Hungarian tag WHEN reading it THEN is Hungarian`() {
        // Given
        val tags = listOf("hu", "hu-HU", "hu_RO", "HU-hu")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.HUNGARIAN }, languages)
    }

    @Test
    fun `GIVEN any Urdu tag WHEN reading it THEN is Urdu`() {
        // Given
        val tags = listOf("ur", "ur-PK", "ur_IN", "UR-pk", "ur-Arab-PK")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.URDU }, languages)
    }

    @Test
    fun `GIVEN any Filipino tag or the legacy Tagalog one WHEN reading it THEN is Filipino`() {
        // Given
        val tags = listOf("fil", "fil-PH", "fil_PH", "FIL-ph", "tl", "tl-PH")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.FILIPINO }, languages)
    }

    @Test
    fun `GIVEN any Serbian tag in the Latin script WHEN reading it THEN is Serbian`() {
        // Given
        val tags = listOf("sr-Latn", "sr-Latn-RS", "SR-latn-me", "sr_Latn_BA")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SERBIAN }, languages)
    }

    @Test
    fun `GIVEN a Serbian tag in Cyrillic or with no script WHEN reading it THEN is no language the app has`() {
        // Given
        val tags = listOf("sr", "sr-RS", "sr-Cyrl", "sr-Cyrl-RS", "sr_ME")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SYSTEM }, languages)
    }

    @Test
    fun `GIVEN any Slovak tag WHEN reading it THEN is Slovak`() {
        // Given
        val tags = listOf("sk", "sk-SK", "sk_SK", "SK-sk")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SLOVAK }, languages)
    }

    @Test
    fun `GIVEN any Persian tag WHEN reading it THEN is Persian`() {
        // Given
        val tags = listOf("fa", "fa-IR", "fa_AF", "FA-ir", "fa-Arab-IR")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PERSIAN }, languages)
    }

    @Test
    fun `GIVEN any Tamil tag WHEN reading it THEN is Tamil`() {
        // Given
        val tags = listOf("ta", "ta-IN", "TA-lk", "ta_SG")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.TAMIL }, languages)
    }

    @Test
    fun `GIVEN any Swahili tag WHEN reading it THEN is Swahili`() {
        // Given
        val tags = listOf("sw", "sw-KE", "sw_TZ", "SW-ug", "sw-CD")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.SWAHILI }, languages)
    }

    @Test
    fun `GIVEN a Traditional Chinese tag WHEN reading it THEN is Traditional Chinese`() {
        // Given
        val tags = listOf("zh-Hant", "zh-TW", "zh_TW", "zh-Hant-TW", "zh-HK", "zh-MO", "ZH-hant", "zh-Hant-CN")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.CHINESE_TRADITIONAL }, languages)
    }

    @Test
    fun `GIVEN a language WHEN reading its spoken code THEN is its bare language but the Traditional tag`() {
        // Given
        val languages = listOf(
            AppLanguage.PORTUGUESE_PORTUGAL,
            AppLanguage.CHINESE_SIMPLIFIED,
            AppLanguage.CHINESE_TRADITIONAL,
            AppLanguage.SYSTEM,
        )

        // When
        val codes = languages.map { it.spokenCode }

        // Then
        assertEquals(listOf("pt", "zh", "zh-Hant", null), codes)
    }

    @Test
    fun `GIVEN any Hebrew tag WHEN reading it THEN is Hebrew with the legacy iw too`() {
        // Given
        val tags = listOf("he", "he-IL", "he_IL", "HE-il", "iw", "iw-IL", "iw_IL")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.HEBREW }, languages)
    }

    @Test
    fun `GIVEN a Portuguese tag of Portugal in other spellings WHEN reading it THEN is the Portuguese of Portugal`() {
        // Given
        val tags = listOf("pt-PT-x-private", "PT-PT", "pt-pt", "pt_pt_x_private", "pt-Latn-pt-u-fw-mon")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE_PORTUGAL }, languages)
    }

    @Test
    fun `GIVEN a Portuguese tag with no region or an odd one WHEN reading it THEN is the Portuguese of Brazil`() {
        // Given
        val tags = listOf("PT", "pt-", "pt--PT", "pt-419", "pt-Latn", "pt-Latn-BR", "pt-x-PT", "pt-PTT")

        // When
        val languages = tags.map(AppLanguage::fromTag)

        // Then
        assertEquals(List(tags.size) { AppLanguage.PORTUGUESE }, languages)
    }

    @Test
    fun `GIVEN every language WHEN its tag is read back THEN is that same language`() {
        // Given
        val languages = AppLanguage.entries

        // When
        val readBack = languages.map { AppLanguage.fromTag(it.tag) }

        // Then
        assertEquals(languages, readBack)
    }

    @Test
    fun `GIVEN every language WHEN drawing the picker chips THEN each shows its code or region and the system none`() {
        // Given
        val languages = AppLanguage.entries

        // When
        val codes = languages.map { it.chipCode }

        // Then
        assertEquals(
            listOf(
                null,
                "EN",
                "BR",
                "PT",
                "419",
                "ES",
                "FR",
                "DE",
                "IT",
                "PL",
                "NL",
                "JA",
                "TR",
                "ID",
                "RU",
                "UA",
                "KO",
                "HI",
                "AR",
                "CN",
                "TW",
                "HE",
                "TH",
                "VI",
                "SV",
                "CS",
                "RO",
                "BN",
                "MS",
                "EL",
                "HR",
                "HU",
                "UR",
                "FIL",
                "SR",
                "SK",
                "FA",
                "TA",
                "SW",
            ),
            codes,
        )
        assertEquals(codes.filterNotNull().size, codes.filterNotNull().distinct().size)
    }

    @Test
    fun `GIVEN odd tags WHEN reading their region THEN is a two letter one before any extension or null`() {
        // Given
        val tags = listOf("", "pt-", "pt--PT", "pt-x-PT", "pt-Latn", "pt-PT-x-BR", "PT_pt", "pt-Latn-br-u-fw-mon")

        // When
        val regions = tags.map(AppLanguage::regionOf)

        // Then
        assertEquals(listOf(null, null, null, null, null, "PT", "PT", "BR"), regions)
    }
}
