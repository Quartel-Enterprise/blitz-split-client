package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale

private const val SPANISH = "es"
private const val SINGULAR_SUFFIX_IO = "ió"
private const val SINGULAR_SUFFIX_O = "ó"
private val irregularSecondPerson = mapOf("deshizo" to "deshiciste", "cambió" to "cambiaste")

/*
 * Why: "Ana dividió", but "Tú dividiste": Spanish conjugates the verb for the person, and one message serves them all.
 * "dividió" becomes "dividiste" and "se unió a" "te uniste a"; any other language keeps the verb as it is. Same rules
 * as the web's and the Apple app's `verbForYou`.
 */
fun toSecondPerson(
    verb: String,
    language: String,
): String {
    if (language != SPANISH) return verb
    val words = verb.split(' ')
    val first = words.first()
    val rest = words.drop(1)
    return when {
        first == "se" && rest.isNotEmpty() ->
            (listOf("te", toSecondPerson(rest.first(), language)) + rest.drop(1)).joinToString(" ")

        first in irregularSecondPerson -> (listOf(irregularSecondPerson.getValue(first)) + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_IO) -> (listOf(first.dropLast(2) + "iste") + rest).joinToString(" ")

        first.endsWith(SINGULAR_SUFFIX_O) -> (listOf(first.dropLast(1) + "aste") + rest).joinToString(" ")

        else -> verb
    }
}

@Composable
fun isSpanishUi(): Boolean = LocalLocale.current.platformLocale.language == SPANISH

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
