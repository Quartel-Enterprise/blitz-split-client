package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale

private const val SPANISH = "es"
private const val FRENCH = "fr"
private const val GERMAN = "de"
private const val ITALIAN = "it"
private const val POLISH = "pl"
private const val DUTCH = "nl"
private const val TURKISH = "tr"
private const val POLISH_THIRD_PERSON_SUFFIX = "ł(a)"
private const val POLISH_SECOND_PERSON_SUFFIX = "łeś(-aś)"
private const val SINGULAR_SUFFIX_IO = "ió"
private const val SINGULAR_SUFFIX_O = "ó"
private val secondPersonAuxiliaries = mapOf(
    FRENCH to mapOf("a" to "as"),
    GERMAN to mapOf("hat" to "hast", "ist" to "bist"),
    ITALIAN to mapOf("ha" to "hai"),
    DUTCH to mapOf("heeft" to "hebt", "is" to "bent"),
)
private val youSubjectLanguages = setOf(SPANISH, FRENCH, GERMAN, ITALIAN, POLISH, DUTCH, TURKISH)
private val turkishPast = Regex("[dt][ıiuü]$")
private val irregularSecondPerson = mapOf(
    "deshizo" to "deshiciste",
    "cambió" to "cambiaste",
    "envió" to "enviaste",
    "inició" to "iniciaste",
    "copió" to "copiaste",
    "vació" to "vaciaste",
    "anunció" to "anunciaste",
)

/*
 * Why: "Ana dividió", but "Tú dividiste": Spanish conjugates the verb for the person, and one message serves them all.
 * "dividió" becomes "dividiste" and "se unió a" "te uniste a". French does the same with its auxiliary, every verb
 * phrase being a passé composé that starts with "a": "Ana a divisé", "Tu as divisé". German, Italian and Dutch too,
 * with their perfect's auxiliary: "Ana hat geteilt", "Du hast geteilt"; "Ana ha diviso", "Tu hai diviso"; "Ana heeft
 * verdeeld", "Jij hebt verdeeld". Polish's past says the gender, which the app doesn't know, so its verb phrases start
 * with a past in "ł(a)": "Ana podzielił(a)", "Ty podzieliłeś(-aś)". Turkish puts its simple past last: "Ana böldü",
 * "Sen böldün". Any other language keeps the verb as it is. Same rules as the web's and the Apple app's `verbForYou`.
 */
fun toSecondPerson(
    verb: String,
    language: String,
): String = when (language) {
    SPANISH -> toSpanishSecondPerson(verb)

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

private fun toSpanishSecondPerson(verb: String): String {
    val words = verb.split(' ')
    val first = words.first()
    val rest = words.drop(1)
    return when {
        first == "se" && rest.isNotEmpty() ->
            (listOf("te", toSpanishSecondPerson(rest.first())) + rest.drop(1)).joinToString(" ")

        first in irregularSecondPerson -> (listOf(irregularSecondPerson.getValue(first)) + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_IO) -> (listOf(first.dropLast(2) + "iste") + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_O) -> (listOf(first.dropLast(1) + "aste") + rest).joinToString(" ")

        else -> verb
    }
}

/*
 * Why: Spanish, French, German, Italian, Polish, Dutch and Turkish say "Tú", "Tu", "Du", "Ty", "Jij" or "Sen" and
 * conjugate the verb for it, unlike the other languages.
 */
@Composable
fun isYouSubjectUi(): Boolean = LocalLocale.current.platformLocale.language in youSubjectLanguages

// Why: only the user is "Tú"; the verb of someone else stays in the third person.
@Composable
fun verbForActorOf(
    verb: String,
    isMe: Boolean,
): String = if (isMe) toSecondPerson(verb, LocalLocale.current.platformLocale.language) else verb

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
