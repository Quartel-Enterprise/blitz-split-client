package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.android.designsystem.R

private const val SPANISH = "es"
private const val PORTUGUESE = "pt"
private const val PORTUGAL_PORTUGUESE = "pt-PT"
private const val FRENCH = "fr"
private const val GERMAN = "de"
private const val ITALIAN = "it"
private const val POLISH = "pl"
private const val DUTCH = "nl"
private const val TURKISH = "tr"
private const val RUSSIAN = "ru"
private const val UKRAINIAN = "uk"
private const val POLISH_THIRD_PERSON_SUFFIX = "ł(a)"
private const val POLISH_SECOND_PERSON_SUFFIX = "łeś(-aś)"
private const val SINGULAR_SUFFIX_IO = "ió"
private const val SINGULAR_SUFFIX_O = "ó"
private val secondPersonAuxiliaries = mapOf(
    FRENCH to mapOf("a" to "as", "l’a" to "l’as", "l'a" to "l'as"),
    GERMAN to mapOf("hat" to "hast", "ist" to "bist"),
    ITALIAN to mapOf("ha" to "hai", "l’ha" to "l’hai", "l'ha" to "l'hai"),
    DUTCH to mapOf("heeft" to "hebt", "is" to "bent"),
)
private val turkishPast = Regex("[dt][ıiuü]$")
private val youSubjectLanguages = setOf(
    SPANISH,
    PORTUGAL_PORTUGUESE,
    FRENCH,
    GERMAN,
    ITALIAN,
    POLISH,
    DUTCH,
    TURKISH,
    RUSSIAN,
    UKRAINIAN,
)
private val spanishObjectPronouns = setOf("lo", "la", "los", "las", "le", "les")
private val portugueseIrregularSecondPerson = mapOf(
    "fez" to "fizeste",
    "desfez" to "desfizeste",
    "saiu" to "saíste",
    "pôs" to "puseste",
)
private val irregularSecondPerson = mapOf(
    "deshizo" to "deshiciste",
    "cambió" to "cambiaste",
    "envió" to "enviaste",
    "inició" to "iniciaste",
    "copió" to "copiaste",
    "vació" to "vaciaste",
    "anunció" to "anunciaste",
    "leyó" to "leíste",
)

/*
 * Why: "Ana dividió", but "Tú dividiste": Spanish conjugates the verb for the person, and one message serves them all.
 * "dividió" becomes "dividiste", "se unió a" "te uniste a" and "lo adjuntó" "lo adjuntaste". French does the same with
 * its auxiliary, every verb phrase being a passé composé that starts with "a" or "l’a": "Ana a divisé", "Tu as divisé". German, Italian and Dutch too,
 * with their perfect's auxiliary: "Ana hat geteilt", "Du hast geteilt"; "Ana ha diviso", "Tu hai diviso"; "Ana heeft
 * verdeeld", "Jij hebt verdeeld". Polish's past says the gender, which the app doesn't know, so its verb phrases start
 * with a past in "ł(a)": "Ana podzielił(a)", "Ty podzieliłeś(-aś)". Turkish puts its simple past last: "Ana böldü",
 * "Sen böldün". Portugal's Portuguese (pt-PT, "tu") ends the verb's preterite in "-aste", "-este" or "-iste": "Ana
 * dividiu", "Tu dividiste". Any other language keeps the verb as it is, Brazil's Portuguese too ("Você dividiu"), and
 * Russian and Ukrainian, whose past says the gender but not the person ("Ана разделил(а)", "Ты разделил(а)"). Same
 * rules as the web's and the Apple app's `verbForYou`.
 */
fun toSecondPerson(
    verb: String,
    language: String,
): String = when (language) {
    SPANISH -> toSpanishSecondPerson(verb)

    PORTUGAL_PORTUGUESE -> toPortugueseSecondPerson(verb)

    POLISH -> replaceFirstWord(verb) { word ->
        if (word.endsWith(POLISH_THIRD_PERSON_SUFFIX)) {
            word.removeSuffix(POLISH_THIRD_PERSON_SUFFIX) + POLISH_SECOND_PERSON_SUFFIX
        } else {
            null
        }
    }

    TURKISH -> toTurkishSecondPerson(verb)

    else -> secondPersonAuxiliaries[language]?.let { auxiliaries -> replaceFirstWord(verb, auxiliaries::get) } ?: verb
}

// Why: the verb phrase with its first word replaced, or as it is when `replacement` has none for that word.
private fun replaceFirstWord(
    verb: String,
    replacement: (String) -> String?,
): String {
    val words = verb.split(' ')
    val first = replacement(words.first()) ?: return verb
    return (listOf(first) + words.drop(1)).joinToString(" ")
}

/*
 * Why: Turkish puts the verb last, so every Turkish verb phrase ends with its simple past, before the colon that
 * leads to the object if there is one ("bir harcamayı böldü:"), and the second person adds an "n" to it whatever the
 * vowel ("böldü" → "böldün", "katıldı" → "katıldın"). A phrase whose clause doesn't end in that past stays as it is.
 */
private fun toTurkishSecondPerson(verb: String): String {
    val clause = verb.substringBefore(':').trimEnd()
    if (!turkishPast.containsMatchIn(clause)) return verb
    return clause + "n" + verb.substring(clause.length)
}

// Why: "dividiu" → "dividiste", "leu" → "leste", "saiu" → "saíste", "fez" → "fizeste"; the first word is the verb.
private fun toPortugueseSecondPerson(verb: String): String = replaceFirstWord(verb) { first ->
    portugueseIrregularSecondPerson[first] ?: when {
        first.endsWith("ou") -> first.dropLast(2) + "aste"
        first.endsWith("eu") -> first.dropLast(2) + "este"
        first.endsWith("iu") -> first.dropLast(2) + "iste"
        else -> null
    }
}

private fun toSpanishSecondPerson(verb: String): String {
    val words = verb.split(' ')
    val first = words.first()
    val rest = words.drop(1)
    return when {
        first == "se" && rest.isNotEmpty() ->
            (listOf("te", toSpanishSecondPerson(rest.first())) + rest.drop(1)).joinToString(" ")

        first in spanishObjectPronouns && rest.isNotEmpty() ->
            (listOf(first, toSpanishSecondPerson(rest.first())) + rest.drop(1)).joinToString(" ")

        first in irregularSecondPerson -> (listOf(irregularSecondPerson.getValue(first)) + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_IO) -> (listOf(first.dropLast(2) + "iste") + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_O) -> (listOf(first.dropLast(1) + "aste") + rest).joinToString(" ")

        else -> verb
    }
}

/*
 * Why: Spanish, Portugal's Portuguese, French, German, Italian, Polish, Dutch and Turkish say "Tú", "Tu", "Du", "Ty",
 * "Jij" or "Sen" and conjugate the verb for it, unlike the other languages. Russian and Ukrainian say "Ты" and "Ти"
 * too, with the verb as it is, rather than a "by you" that would decline the names of everyone else ("удалено Иваном").
 */
@Composable
fun isYouSubjectUi(): Boolean = secondPersonLanguageUi() in youSubjectLanguages

/*
 * Why: the language alone, but Portugal's Portuguese is told from Brazil's by the strings Android drew: its resources
 * also answer pt-AO, pt-MZ and the like, which no country check would catch.
 */
@Composable
private fun secondPersonLanguageUi(): String {
    val locale = LocalLocale.current.platformLocale
    val isPortugal = LocalContext.current.resources.getBoolean(R.bool.designsystem_portugal_portuguese)
    return if (locale.language == PORTUGUESE && isPortugal) PORTUGAL_PORTUGUESE else locale.language
}

// Why: only the user is "Tú"; the verb of someone else stays in the third person.
@Composable
fun verbForActorOf(
    verb: String,
    isMe: Boolean,
): String = if (isMe) toSecondPerson(verb, secondPersonLanguageUi()) else verb

/*
 * Why: in a sentence template "%1$s dividió %2$s" the actor placeholder starts it, and with the user as the actor the
 * verb phrase right after it (up to the next placeholder) is said to "Tú".
 */
@Composable
fun templateForActorOf(
    template: String,
    isMe: Boolean,
    actorPlaceholder: String = "%1\$s",
): String {
    val actor = "$actorPlaceholder "
    if (!isMe || !template.startsWith(actor)) return template
    val rest = template.removePrefix(actor)
    val phrase = rest.substringBefore('%')
    return actor + verbForActorOf(phrase, isMe = true) + rest.removePrefix(phrase)
}
