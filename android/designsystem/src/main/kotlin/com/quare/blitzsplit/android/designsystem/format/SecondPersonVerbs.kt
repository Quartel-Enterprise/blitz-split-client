package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.android.designsystem.R
import java.util.Locale

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
private const val KOREAN = "ko"
private const val KOREAN_HONORIFIC_SUBJECT = "님이"
private const val HINDI = "hi"
private const val HINDI_ERGATIVE = "ने"
private const val VIETNAMESE = "vi"
private const val SWEDISH = "sv"
private const val NORWEGIAN = "nb"
private const val CZECH = "cs"
private const val CZECH_SECOND_PERSON_AUXILIARY = "jsi"
private const val SLOVAK = "sk"
private const val SLOVAK_SECOND_PERSON_AUXILIARY = "si"
private const val BULGARIAN = "bg"
private const val CATALAN = "ca"
private const val DANISH = "da"
private const val FINNISH = "fi"
private const val LATVIAN = "lv"
private const val LATVIAN_REFLEXIVE_PAST = "ās"
private const val LATVIAN_PAST = "a"
private const val ESTONIAN = "et"
private const val LITHUANIAN = "lt"
private val lithuanianPast = Regex("""^(\S*?)([oė])(si)?(?=[\s,:]|$)""")
private val czechPast = Regex("""^\S*l\(a\)(?=[\s,:]|$)""")
private const val ROMANIAN = "ro"
private const val BENGALI = "bn"
private const val GREEK = "el"
private const val CROATIAN = "hr"
private const val SERBIAN = "sr"
private const val LATIN_SCRIPT = "Latn"
private const val URDU = "ur"
private const val PERSIAN = "fa"
private val greekPast = Regex("""^(?:(?:το|τη|την|τον|τα|τους|τις) )?\S*ε(?=[\s,:]|$)""")
private const val HUNGARIAN = "hu"
private const val FILIPINO = "fil"
private const val LEGACY_TAGALOG = "tl"
private val firstWord = Regex("""^[^\s,:]+""")
private val hungarianDefinitePast = Regex("""t[ae]$""")
private val hungarianIndefinitePast = mapOf(
    "felosztott" to "felosztottál",
    "szerkesztett" to "szerkesztettél",
    "törölt" to "töröltél",
    "rögzített" to "rögzítettél",
    "visszavont" to "visszavontál",
    "hozzáadott" to "hozzáadtál",
    "csatlakozott" to "csatlakoztál",
    "kilépett" to "kiléptél",
    "eltávolított" to "eltávolítottál",
    "átnevezett" to "átneveztél",
    "hozzászólt" to "hozzászóltál",
    "csatolt" to "csatoltál",
    "kért" to "kértél",
    "küldött" to "küldtél",
    "létrehozott" to "létrehoztál",
    "módosított" to "módosítottál",
    "megváltoztatott" to "megváltoztattál",
    "elolvasott" to "elolvastál",
    "bekapcsolt" to "bekapcsoltál",
    "kikapcsolt" to "kikapcsoltál",
    "fizetett" to "fizettél",
)
private const val POLISH_THIRD_PERSON_SUFFIX = "ł(a)"
private const val POLISH_SECOND_PERSON_SUFFIX = "łeś(-aś)"
private const val SINGULAR_SUFFIX_IO = "ió"
private const val SINGULAR_SUFFIX_O = "ó"
private val secondPersonAuxiliaries = mapOf(
    FRENCH to mapOf("a" to "as", "l’a" to "l’as", "l'a" to "l'as"),
    GERMAN to mapOf("hat" to "hast", "ist" to "bist"),
    ITALIAN to mapOf("ha" to "hai", "l’ha" to "l’hai", "l'ha" to "l'hai"),
    DUTCH to mapOf("heeft" to "hebt", "is" to "bent"),
    ROMANIAN to mapOf("a" to "ai"),
    CROATIAN to mapOf("je" to "si"),
    SERBIAN to mapOf("je" to "si"),
    FINNISH to mapOf("on" to "olet"),
    ESTONIAN to mapOf("on" to "oled"),
    CATALAN to mapOf("ha" to "has", "l’ha" to "l’has", "l'ha" to "l'has"),
)
private val turkishPast = Regex("[dt][ıiuü]$")
private val persianPast = Regex("[دت]$")
private const val PERSIAN_SECOND_PERSON_ENDING = "ید"
private const val TAMIL = "ta"
private const val TELUGU = "te"
private const val MARATHI = "mr"
private const val MARATHI_RESPECTFUL_AGENT = "यांनी"
private const val TAMIL_THIRD_PERSON_PAST = "ார்"
private val tamilPast = Regex("$TAMIL_THIRD_PERSON_PAST$")
private const val SWAHILI = "sw"
private val swahiliThirdPersonPrefix = Regex("""^a(?=(?:me|li)\S)""")
private const val TAMIL_SECOND_PERSON_PAST = "ீர்கள்"
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
    KOREAN,
    HINDI,
    VIETNAMESE,
    SWEDISH,
    CZECH,
    ROMANIAN,
    BENGALI,
    GREEK,
    CROATIAN,
    SERBIAN,
    SLOVAK,
    NORWEGIAN,
    BULGARIAN,
    CATALAN,
    DANISH,
    FINNISH,
    LATVIAN,
    ESTONIAN,
    LITHUANIAN,
    HUNGARIAN,
    URDU,
    PERSIAN,
    FILIPINO,
    LEGACY_TAGALOG,
    TAMIL,
    TELUGU,
    MARATHI,
    SWAHILI,
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
 * its auxiliary, every verb phrase being a passé composé that starts with "a" or "l’a": "Ana a divisé", "Tu as divisé".
 * German, Italian and Dutch too, with their perfect's auxiliary: "Ana hat geteilt", "Du hast geteilt"; "Ana ha diviso",
 * "Tu hai diviso"; "Ana heeft verdeeld", "Jij hebt verdeeld"; Romanian's "a" too: "Ana a împărțit", "Tu ai împărțit";
 * Catalan's "ha" too, every Catalan verb phrase being a perfect that starts with "ha" or "l’ha": "Anna ha dividit",
 * "Tu has dividit"; and Croatian's "je", every Croatian verb phrase starting with it before a past written "-o/la", the app not knowing
 * the gender: "Ana je podijelio/la", "Ti si podijelio/la"; Finnish's "on": "Ana on jakanut", "Sinä olet jakanut"; and Estonian's "on": "Ana on jaganud", "Sina oled jaganud".
 * Czech says the person with an auxiliary before its past, which the third person has none of: "Ana rozdělil(a)", "Ty
 * jsi rozdělil(a)", and Slovak likewise with "si": "Ana rozdelil(a)", "Ty si rozdelil(a)". Polish's past says the gender, which the app doesn't know, so its verb phrases start with a past in
 * "ł(a)": "Ana podzielił(a)", "Ty podzieliłeś(-aś)". Turkish puts its simple past last: "Ana böldü", "Sen böldün",
 * and Persian too, with the respectful "ید" of "شما": "Ana هزینه‌ای را تقسیم کرد", "شما هزینه‌ای را تقسیم کردید".
 * Portugal's Portuguese (pt-PT, "tu") ends the verb's preterite in "-aste", "-este" or "-iste": "Ana dividiu", "Tu
 * dividiste". Any other language keeps the verb as it is, Brazil's Portuguese too ("Você dividiu"), and Russian and
 * Ukrainian, whose past says the gender but not the person ("Ана разделил(а)", "Ты разделил(а)"). Korean doesn't
 * conjugate for the person either, but writes someone else as "Ana 님이" (님 always takes 이, so no particle hangs on the
 * sound of a name) and the user as "내가", the user's subject word holding its own particle: every Korean verb phrase
 * starts with "님이", which the user's drops ("Ana 님이 나눴어요", "내가 나눴어요"). Hindi's past puts the ergative "ने" after the
 * actor, written apart after a name and as one word with the user's "आपने": every Hindi verb phrase starts with "ने",
 * which the user's drops ("Ana ने खर्च बांटा", "आपने खर्च बांटा"). Greek ends its aorist in "-ε" for someone else and "-ες"
 * for the user, an object pronoun staying before it: "Ana μοίρασε", "Εσύ μοίρασες"; "το επισύναψε", "το επισύναψες".
 * Hungarian starts every verb phrase with a past: a definite one in "-ta"/"-te" takes a "d" for the user ("Ana
 * létrehozta", "Te létrehoztad"), an indefinite one is one of a short list of pasts a translation may start with,
 * each with its own second person ("Ana felosztott", "Te felosztottál"; "kilépett", "kiléptél").
 * Bengali needs no rule: its respectful verb is the same for "আপনি" and for a name ("Ana ভাগ করেছেন", "আপনি ভাগ করেছেন"),
 * and Malay's verb doesn't change with the person. Urdu needs none either: its ergative "نے" is written apart after
 * a name and after the user's "آپ" alike ("Ana نے خرچ بانٹا", "آپ نے خرچ بانٹا"). Bulgarian needs none either: its aorist, which every Bulgarian verb phrase
 * starts with, is the same for the user's "Ти" and for a name ("Ana раздели", "Ти раздели"). Filipino needs none: its "ay" and
 * its actor-focus verb are the same for the user's "Ikaw" and for a name ("Ana ay nagbahagi", "Ikaw ay nagbahagi").
 * Tamil puts its verb last too, ending every verb phrase, before the colon that leads to the object, with a
 * respectful past in "-ார்", which the user's turns into "-ீர்கள்" ("Ana ஒரு செலவைப் பிரித்தார்", "நீங்கள் ஒரு
 * செலவைப் பிரித்தீர்கள்"). Telugu needs no rule: its respectful past "-ారు" is the same for "మీరు" and for a name
 * ("Ana ఒక ఖర్చును పంచారు", "మీరు ఒక ఖర్చును పంచారు"). Marathi does as Hindi with the respectful agent "यांनी", written
 * apart after a name, which the user's "तुम्ही" doesn't take: every Marathi verb phrase starts with "यांनी", which the
 * user's drops ("Ana यांनी खर्च वाटला", "तुम्ही खर्च वाटला"). Swahili's verb carries the subject's prefix, so every Swahili
 * verb phrase starts with a perfect "a-me-" or a past "a-li-", whose "a" the user's turns into "u": "Ana amegawanya",
 * "Wewe umegawanya". Latvian starts every verb phrase with a simple past, which says the person but not the gender: its
 * third person in "-a" ends in "-i" for the user, a reflexive one in "-ās" in "-ies" ("Ana sadalīja", "Tu sadalīji";
 * "Ana pievienojās", "Tu pievienojies"). Lithuanian's simple past, which says no gender and which every Lithuanian
 * verb phrase starts with, ends in "-o" or "-ė" ("-osi", "-ėsi" when reflexive) for someone else and "-ai" or "-ei"
 * ("-aisi", "-eisi") for the user: "Ana padalijo", "Tu padalijai"; "ištrynė" → "ištrynei", "dalijosi" → "dalijaisi".
 * Same rules as the web's and the Apple app's `verbForYou`.
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

    TURKISH -> toVerbLastSecondPerson(verb, turkishPast, "n")

    PERSIAN -> toVerbLastSecondPerson(verb, persianPast, PERSIAN_SECOND_PERSON_ENDING)

    TAMIL -> toVerbLastSecondPerson(verb, tamilPast, TAMIL_SECOND_PERSON_PAST, dropped = TAMIL_THIRD_PERSON_PAST)

    KOREAN -> dropSubjectParticle(verb, KOREAN_HONORIFIC_SUBJECT)

    HINDI -> dropSubjectParticle(verb, HINDI_ERGATIVE)

    MARATHI -> dropSubjectParticle(verb, MARATHI_RESPECTFUL_AGENT)

    CZECH -> if (czechPast.containsMatchIn(verb)) "$CZECH_SECOND_PERSON_AUXILIARY $verb" else verb

    SLOVAK -> if (czechPast.containsMatchIn(verb)) "$SLOVAK_SECOND_PERSON_AUXILIARY $verb" else verb

    GREEK -> greekPast.replace(verb) { match -> match.value + "ς" }

    HUNGARIAN -> firstWord.replace(verb) { match -> toHungarianSecondPerson(match.value) }

    SWAHILI -> swahiliThirdPersonPrefix.replace(verb, "u")

    LATVIAN -> firstWord.replace(verb) { match -> toLatvianSecondPerson(match.value) }

    LITHUANIAN -> lithuanianPast.replace(verb) { match ->
        val (stem, vowel, reflexive) = match.destructured
        stem + (if (vowel == "o") "ai" else "ei") + reflexive
    }

    else -> secondPersonAuxiliaries[language]?.let { auxiliaries -> replaceFirstWord(verb, auxiliaries::get) } ?: verb
}

// Why: "sadalīja" → "sadalīji", "pievienojās" → "pievienojies"; a word that is no third-person past stays as it is.
private fun toLatvianSecondPerson(past: String): String = when {
    past.endsWith(LATVIAN_REFLEXIVE_PAST) -> past.removeSuffix(LATVIAN_REFLEXIVE_PAST) + "ies"
    past.endsWith(LATVIAN_PAST) -> past.removeSuffix(LATVIAN_PAST) + "i"
    else -> past
}

// Why: "felosztott" → "felosztottál", "létrehozta" → "létrehoztad"; any other word stays as it is.
private fun toHungarianSecondPerson(past: String): String = hungarianIndefinitePast[past]
    ?: if (hungarianDefinitePast.containsMatchIn(past)) past + "d" else past

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
 * Why: Turkish and Persian put the verb last, so every verb phrase of theirs ends with its simple past, before the
 * colon that leads to the object if there is one ("bir harcamayı böldü:", "هزینه‌ای را تقسیم کرد:"), and the second
 * person adds its ending to it: Turkish an "n" whatever the vowel ("böldü" → "böldün", "katıldı" → "katıldın"),
 * Persian, whose past stem ends in "د" or "ت", the respectful "ید" ("تقسیم کرد" → "تقسیم کردید", "پیوست" → "پیوستید").
 * Tamil's respectful past drops its "-ார்" for the user's "-ீர்கள்" ("பிரித்தார்" → "பிரித்தீர்கள்"): `dropped`. A
 * phrase whose clause doesn't end in that past stays as it is.
 */
private fun toVerbLastSecondPerson(
    verb: String,
    past: Regex,
    ending: String,
    dropped: String = "",
): String {
    val clause = verb.substringBefore(':').trimEnd()
    if (!past.containsMatchIn(clause)) return verb
    return clause.removeSuffix(dropped) + ending + verb.substring(clause.length)
}

/*
 * Why: "님이 나눴어요" → "나눴어요", "ने खर्च बांटा" → "खर्च बांटा", "님이 " → "": the user's subject word ("내가",
 * "आपने") already holds the particle.
 */
private fun dropSubjectParticle(
    verb: String,
    particle: String,
): String {
    val words = verb.split(' ')
    return if (words.first() == particle) words.drop(1).joinToString(" ") else verb
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
 * Korean says "내가" rather than a "by you" that would put "님이" after "나", and Hindi "आपने" rather than "आपके द्वारा".
 * Vietnamese says "Bạn đã xóa…", as it says every other row of the user's, its verb not changing with the person, and
 * Swedish "Du raderade…", Norwegian "Du slettet…", Danish "Du slettede…", Finnish "Sinä olet poistanut…" and Estonian "Sina oled kustutanud…" (with their verb rule), Latvian "Tu izdzēsi…" (with its own), Lithuanian "Tu ištrynei…" (with its verb rule), Bengali "আপনি মুছে ফেলেছেন…" and Urdu "آپ نے حذف کیا…" likewise. Persian says "شما
 * حذف کردید…", with its verb rule. Czech says "Ty jsi
 * smazal(a)…", Slovak "Ty si vymazal(a)…", Bulgarian "Ти изтри…", Catalan "Tu has suprimit…", Romanian "Tu ai șters…", Greek "Εσύ διέγραψες…", Hungarian "Te törölted…" and Croatian "Ti si
 * izbrisao/la…", with their verb rule, Croatian also so as not to decline the names of everyone else after "by"
 * ("od Ane"). Filipino says "Ikaw ay nagbura…", its verb not changing with the person, rather than a "by you" that
 * would need "ni" for a name and "mo" for the user. Tamil says "நீங்கள் நீக்கினீர்கள்: …", with its verb rule, and
 * Telugu "మీరు తొలగించారు: …", its verb not changing with the person, Marathi "तुम्ही … हटवला", with its rule, and
 * Swahili "Wewe ulifuta…", with its own.
 */
@Composable
fun isYouSubjectUi(): Boolean = secondPersonLanguageUi() in youSubjectLanguages

/*
 * Why: the language alone, but Portugal's Portuguese is told from Brazil's by the strings Android drew: its resources
 * also answer pt-AO, pt-MZ and the like, which no country check would catch. Norwegian is told by them too, since a
 * Nynorsk device ("nn") reaching values-nb through its fallback languages draws Bokmål.
 */
@Composable
private fun secondPersonLanguageUi(): String {
    val locale = LocalLocale.current.platformLocale
    val resources = LocalContext.current.resources
    val isPortugal = resources.getBoolean(R.bool.designsystem_portugal_portuguese)
    return when {
        locale.language == PORTUGUESE && isPortugal -> PORTUGAL_PORTUGUESE
        resources.getBoolean(R.bool.designsystem_norwegian) -> NORWEGIAN
        isCyrillicSerbian(locale) -> ""
        else -> locale.language
    }
}

// Why: a Serbian device in Cyrillic draws the English strings, the app having the Latin resources only.
private fun isCyrillicSerbian(locale: Locale): Boolean = locale.language == SERBIAN && locale.script != LATIN_SCRIPT

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
