import SwiftUI

/// Spanish says "Ana dividió" but "Tú dividiste", French "Ana a divisé" but "Tu as divisé", German "Ana hat geteilt"
/// but "Du hast geteilt", Italian "Ana ha diviso" but "Tu hai diviso", Dutch "Ana heeft verdeeld" but "Jij hebt
/// verdeeld", Polish "Ana podzielił(a)" but "Ty podzieliłeś(-aś)" and Turkish "Ana böldü" but "Sen böldün": one
/// localized sentence serves every actor, so when the actor is this device's user the verb of the "Tú", "Tu", "Du",
/// "Jij", "Ty" or "Sen" is conjugated to the second person, and so is Portugal's Portuguese ("Ana dividiu", "Tu
/// dividiste"). Other languages are left as they are, Brazil's Portuguese too ("Você dividiu"), and Russian and
/// Ukrainian, whose past says the gender but not the person ("Ты разделил(а)"). Korean writes someone else as "Ana 님이"
/// (님 always takes 이, so no particle hangs on the sound of a name) and the user as "내가", whose subject word holds its
/// own particle, so its verb phrases all start with "님이", which the user's drops ("Ana 님이 나눴어요", "내가 나눴어요"). Hindi's
/// past puts the ergative "ने" after the actor, apart after a name and one word with the user's "आपने", so its verb
/// phrases all start with "ने", which the user's drops ("Ana ने खर्च बांटा", "आपने खर्च बांटा"). Romanian swaps its
/// perfect's auxiliary as French does ("Ana a împărțit", "Tu ai împărțit"), as Catalan does ("Anna ha dividit", "Tu has dividit"), and Czech adds one before the past ("Ana
/// rozdělil(a)", "Ty jsi rozdělil(a)"), as Slovak does with "si" ("Ana rozdelil(a)", "Ty si rozdelil(a)"). Greek ends its aorist in "-ε" for someone else and "-ες" for the user ("Ana
/// μοίρασε", "Εσύ μοίρασες"). Croatian swaps its "je" for "si" before a past written "-o/la", the app not knowing the
/// gender ("Ana je podijelio/la", "Ti si podijelio/la"), Finnish its "on" for "olet" ("Ana on jakanut", "Sinä olet
/// jakanut"), and Estonian its "on" for "oled" ("Ana on jaganud", "Sina oled jaganud"). Hungarian starts each verb phrase with a past: a definite one
/// in "-ta"/"-te" takes a "d" for the user ("Ana létrehozta", "Te létrehoztad"), an indefinite one is one of a short
/// list a translation may start with, each with its own second person ("Ana felosztott", "Te felosztottál"). Bengali's
/// respectful verb is the same for a name and for "আপনি", and Malay's doesn't change.
/// Urdu writes its ergative "نے" apart after a name and after the user's "آپ" alike ("Ana نے خرچ بانٹا", "آپ نے خرچ
/// بانٹا"), so its verb stays as it is. Persian puts its simple past last as Turkish does, and the user's respectful
/// "شما" adds "ید" to it ("Ana هزینه‌ای را تقسیم کرد", "شما هزینه‌ای را تقسیم کردید").
/// Bulgarian's aorist is the same for a name and for "Ти" ("Ana раздели", "Ти
/// раздели"), so its verb stays as it is too.
///
/// Telugu's respectful past "-ారు" is the same for a name and for "మీరు", so its verb stays as it is. Marathi does as
/// Hindi with its respectful agent "यांनी", which the user's "तुम्ही" doesn't take, so its verb phrases all start with
/// "यांनी", which the user's drops ("Ana यांनी खर्च वाटला", "तुम्ही खर्च वाटला").
///
/// A sentence is localized with the placeholders `actor` and `object` in place of the names, so its verb can be
/// changed before the names, bold or not, go in. The same rules as the web's and Android's `toSecondPerson`.
enum SecondPerson {
    /// Stand-ins for the actor and the object while the sentence is localized: private-use characters no copy has.
    static let actor = "\u{E000}"
    static let object = "\u{E001}"

    private static let portugueseIrregular = ["fez": "fizeste", "desfez": "desfizeste", "saiu": "saíste", "pôs": "puseste"]

    /// The language the verb is conjugated for: its two letters, but Portugal's Portuguese is told from Brazil's.
    private static var appSecondPersonLanguage: String? {
        Bundle.appLanguageIdentifier == "pt-PT" ? "pt-PT" : Bundle.appLanguageCode
    }

    private static let irregular = [
        "deshizo": "deshiciste", "cambió": "cambiaste",
        // Why: an -iar verb ends its preterite in "-ió" but takes "-aste", unlike the -er and -ir verbs.
        "envió": "enviaste", "inició": "iniciaste", "copió": "copiaste", "vació": "vaciaste", "anunció": "anunciaste",
        "leyó": "leíste",
    ]

    /// The object pronoun Spanish puts before the verb ("lo adjuntó"), which stays as the verb changes ("lo adjuntaste").
    private static let spanishObjectPronouns: Set = ["lo", "la", "los", "las", "le", "les"]

    /// The indefinite pasts Hungarian's verb phrases start with, with their second person.
    private static let hungarianIndefinitePast = [
        "felosztott": "felosztottál", "szerkesztett": "szerkesztettél", "törölt": "töröltél",
        "rögzített": "rögzítettél", "visszavont": "visszavontál", "hozzáadott": "hozzáadtál",
        "csatlakozott": "csatlakoztál", "kilépett": "kiléptél", "eltávolított": "eltávolítottál",
        "átnevezett": "átneveztél", "hozzászólt": "hozzászóltál", "csatolt": "csatoltál", "kért": "kértél",
        "küldött": "küldtél", "létrehozott": "létrehoztál", "módosított": "módosítottál",
        "megváltoztatott": "megváltoztattál", "elolvasott": "elolvastál", "bekapcsolt": "bekapcsoltál",
        "kikapcsolt": "kikapcsoltál", "fizetett": "fizettél",
    ]

    /// Czech's and Slovak's second-person auxiliary, put before a past in "l(a)" that the third person says without one.
    private static let pastAuxiliaries = ["cs": "jsi", "sk": "si"]

    /// Each language's perfect auxiliary in the third person, with its second person.
    private static let auxiliaries = [
        "fr": ["a": "as", "l’a": "l’as", "l'a": "l'as"],
        "de": ["hat": "hast", "ist": "bist"],
        "it": ["ha": "hai", "l’ha": "l’hai", "l'ha": "l'hai"],
        "nl": ["heeft": "hebt", "is": "bent"],
        "ro": ["a": "ai"],
        "hr": ["je": "si"],
        "sr": ["je": "si"],
        "fi": ["on": "olet"],
        "et": ["on": "oled"],
        "ca": ["ha": "has", "l’ha": "l’has", "l'ha": "l'has"],
    ]

    /// Spanish: "dividió" → "dividiste", "se unió a" → "te uniste a", "cambió la división de" → "cambiaste la división de".
    /// French, German, Italian, Dutch, Romanian, Croatian, Catalan ("ha dividit" → "has dividit"), Finnish and Estonian: every verb phrase is a perfect that starts with its
    /// auxiliary, so "a divisé" → "as divisé", "hat geteilt" → "hast geteilt", "ist beigetreten" → "bist beigetreten",
    /// "ha diviso" → "hai diviso", "heeft verdeeld" → "hebt verdeeld", "is lid geworden" → "bent lid geworden", "a
    /// împărțit" → "ai împărțit", "je podijelio/la" → "si podijelio/la", "on jakanut" → "olet jakanut", "on jaganud" → "oled jaganud". Czech: the second person's past takes the
    /// auxiliary "jsi" the third person has none of, so "rozdělil(a)" → "jsi rozdělil(a)"; a phrase that doesn't start
    /// with that past stays as it is. Slovak likewise with "si": "rozdelil(a)" → "si rozdelil(a)".
    /// Portugal's Portuguese: "dividiu" → "dividiste", "leu" → "leste", "saiu" → "saíste", "fez" → "fizeste".
    /// Polish: every verb phrase starts with a past in "ł(a)", the app not knowing the gender, so "podzielił(a)" →
    /// "podzieliłeś(-aś)". Turkish: every verb phrase ends with its simple past, which takes an "n", so "böldü:" →
    /// "böldün:". Persian: every verb phrase ends with its simple past, which takes the respectful "ید" of "شما", so
    /// "تقسیم کرد:" → "تقسیم کردید:". Korean: every verb phrase starts with "님이" after the actor, which the user's drops, since the user's
    /// subject word ("내가") holds its own particle, so "님이 나눴어요" → "나눴어요". Hindi: the same with "ने", the user's
    /// "आपने" holding it, so "ने खर्च बांटा" → "खर्च बांटा". Greek: the aorist takes an "ς", so "μοίρασε" → "μοίρασες".
    /// Hungarian: the past the phrase starts with takes a "d" in "-ta"/"-te" ("létrehozta" → "létrehoztad"), or its
    /// listed second person ("felosztott" → "felosztottál"). Swahili: the third person's "a" of the perfect or past the
/// phrase starts with turns into "u", so "amegawanya" → "umegawanya", "alifuta" → "ulifuta". Another language keeps
/// the verb as it is.
    static func verbForYou(_ verb: String, language: String? = appSecondPersonLanguage) -> String {
        switch language {
        case "es": return spanishVerbForYou(verb)
        case "pt-PT": return portugueseVerbForYou(verb)
        case "tr": return turkishVerbForYou(verb)
        case "fa": return verbLastForYou(verb, past: "[دت]$", ending: "ید")
        case "ta": return verbLastForYou(verb, past: "ார்$", ending: "ீர்கள்", dropped: "ார்")
        case "ko": return verbWithoutSubjectParticle(verb, particle: "님이")
        case "hi": return verbWithoutSubjectParticle(verb, particle: "ने")
        case "mr": return verbWithoutSubjectParticle(verb, particle: "यांनी")
        case let .some(code) where pastAuxiliaries[code] != nil:
            // Why: the past may be followed by a comma or a colon ("změnil(a), kdo platil").
            guard verb.range(of: #"^\S*l\(a\)(?=[\s,:]|$)"#, options: .regularExpression) != nil,
                  let auxiliary = pastAuxiliaries[code] else { return verb }
            return auxiliary + " " + verb
        case "el":
            // Why: an object pronoun stays before the verb ("το επισύναψε"), which a comma or a colon may follow.
            guard let past = verb.range(of: #"^((το|τη|την|τον|τα|τους|τις) )?\S*ε(?=[\s,:]|$)"#, options: .regularExpression)
            else { return verb }
            return verb.replacingCharacters(in: past, with: verb[past] + "ς")
        case "hu":
            // Why: the past may be followed by a comma or a colon ("csatolta ma, 21:10").
            guard let past = verb.range(of: #"^[^\s,:]+"#, options: .regularExpression) else { return verb }
            let word = String(verb[past])
            if let known = hungarianIndefinitePast[word] { return verb.replacingCharacters(in: past, with: known) }
            guard word.hasSuffix("ta") || word.hasSuffix("te") else { return verb }
            return verb.replacingCharacters(in: past, with: word + "d")
        case "sw":
            // Why: "amegawanya" → "umegawanya", "alilipa" → "ulilipa": only the subject prefix changes.
            guard verb.range(of: #"^a(?=(me|li)\S)"#, options: .regularExpression) != nil else { return verb }
            return "u" + verb.dropFirst()
        case "pl": return replacingFirstWord(of: verb) { word in
                word.hasSuffix("ł(a)") ? String(word.dropLast("ł(a)".count)) + "łeś(-aś)" : nil
            }
        case let .some(code) where auxiliaries[code] != nil:
            return replacingFirstWord(of: verb) { auxiliaries[code]?[$0] }
        default: return verb
        }
    }

    /// Korean and Hindi: "님이 나눴어요" → "나눴어요", "ने खर्च बांटा" → "खर्च बांटा", "님이 " → "": the user's subject word
    /// ("내가", "आपने") already holds the particle.
    private static func verbWithoutSubjectParticle(_ verb: String, particle: String) -> String {
        guard verb.hasPrefix(particle) else { return verb }
        let rest = verb.dropFirst(particle.count)
        guard rest.isEmpty || rest.first == " " else { return verb }
        return String(rest.dropFirst())
    }

    /// The verb phrase with its first word replaced, or as it is when `replacement` has none for that word.
    private static func replacingFirstWord(of verb: String, with replacement: (String) -> String?) -> String {
        var words = verb.split(separator: " ", omittingEmptySubsequences: false).map(String.init)
        guard let first = words.first, let replaced = replacement(first) else { return verb }
        words[0] = replaced
        return words.joined(separator: " ")
    }

    /// Turkish puts the verb last, so every Turkish verb phrase ends with its simple past, before the colon that leads to
    /// the object if there is one ("bir harcamayı böldü:"), and the second person adds an "n" to it whatever the vowel
    /// ("böldü" → "böldün", "katıldı" → "katıldın"). A phrase whose clause doesn't end in that past stays as it is.
    private static func turkishVerbForYou(_ verb: String) -> String {
        verbLastForYou(verb, past: "[dt][ıiuü]$", ending: "n")
    }

    /// A language that puts its verb last (Turkish, Persian): the clause before the colon that leads to the object, if
    /// any, ends with a simple past matching `past`, which takes `ending` for the user. Persian's past stem ends in "د"
    /// or "ت" and takes the respectful "ید" ("تقسیم کرد" → "تقسیم کردید", "پیوست" → "پیوستید"). Tamil's respectful past
    /// drops its "-ார்" (`dropped`) for the user's "-ீர்கள்" ("பிரித்தார்" → "பிரித்தீர்கள்"). A phrase whose clause
    /// doesn't end in that past stays as it is.
    private static func verbLastForYou(_ verb: String, past: String, ending: String, dropped: String = "") -> String {
        var end = verb.firstIndex(of: ":") ?? verb.endIndex
        while end > verb.startIndex, verb[verb.index(before: end)].isWhitespace {
            end = verb.index(before: end)
        }
        let clause = verb[..<end]
        guard clause.range(of: past, options: .regularExpression) != nil else { return verb }
        // Why: the Unicode scalars, since Swift's Character groups a vowel sign ("ா") with the consonant before it.
        var stem = String.UnicodeScalarView(clause.unicodeScalars)
        if stem.reversed().starts(with: dropped.unicodeScalars.reversed()) {
            stem.removeLast(dropped.unicodeScalars.count)
        }
        return String(stem) + ending + String(verb[end...])
    }

    private static func portugueseVerbForYou(_ verb: String) -> String {
        replacingFirstWord(of: verb) { first in
            if let known = portugueseIrregular[first] { return known }
            for (suffix, ending) in [("ou", "aste"), ("eu", "este"), ("iu", "iste")] where first.hasSuffix(suffix) {
                return String(first.dropLast(suffix.count)) + ending
            }
            return nil
        }
    }

    private static func spanishVerbForYou(_ verb: String) -> String {
        var words = verb.split(separator: " ", omittingEmptySubsequences: false).map(String.init)
        guard let first = words.first else { return verb }
        if first == "se", words.count > 1 {
            words[1] = spanishVerbForYou(words[1])
            words[0] = "te"
        } else if spanishObjectPronouns.contains(first), words.count > 1 {
            words[1] = spanishVerbForYou(words[1])
        } else if let known = irregular[first] {
            words[0] = known
        } else if first.hasSuffix("ió") {
            words[0] = String(first.dropLast(2)) + "iste"
        } else if first.hasSuffix("ó") {
            words[0] = String(first.dropLast(1)) + "aste"
        }
        return words.joined(separator: " ")
    }

    /// The localized sentence with the verb right after the actor said to the user when the actor is the user, in the
    /// languages `verbForYou` conjugates.
    static func template(_ template: String, actorIsMe: Bool) -> String {
        guard actorIsMe, template.hasPrefix(actor + " ") else { return template }
        let rest = template.dropFirst(actor.count + 1)
        let phrase = rest.prefix { $0 != Character(object) }
        return actor + " " + verbForYou(String(phrase)) + rest.dropFirst(phrase.count)
    }

    /// The sentence as a string, with the names in.
    static func sentence(
        _ template: String,
        actor name: String,
        object objectName: String = "",
        actorIsMe: Bool,
    ) -> String {
        Self.template(template, actorIsMe: actorIsMe)
            .replacingOccurrences(of: actor, with: name)
            .replacingOccurrences(of: object, with: objectName)
    }

    /// The sentence as a Text, with the names in as the Texts given so they keep their weight.
    static func text(
        _ template: String,
        actor actorText: Text,
        object objectText: Text? = nil,
        actorIsMe: Bool,
    ) -> Text {
        let sentence = Self.template(template, actorIsMe: actorIsMe)
        var interpolation = LocalizedStringKey.StringInterpolation(
            literalCapacity: sentence.count,
            interpolationCount: 2,
        )
        var literal = ""
        for character in sentence {
            let piece = String(character)
            guard piece == actor || piece == object else {
                literal.append(character)
                continue
            }
            if !literal.isEmpty { interpolation.appendLiteral(literal) }
            literal = ""
            interpolation.appendInterpolation(piece == actor ? actorText : objectText ?? Text(verbatim: ""))
        }
        if !literal.isEmpty { interpolation.appendLiteral(literal) }
        return Text(LocalizedStringKey(stringInterpolation: interpolation))
    }
}
