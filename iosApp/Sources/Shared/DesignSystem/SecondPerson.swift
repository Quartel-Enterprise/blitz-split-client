import SwiftUI

/// Spanish says "Ana dividió" but "Tú dividiste", French "Ana a divisé" but "Tu as divisé", German "Ana hat geteilt"
/// but "Du hast geteilt", Italian "Ana ha diviso" but "Tu hai diviso", Dutch "Ana heeft verdeeld" but "Jij hebt
/// verdeeld", Polish "Ana podzielił(a)" but "Ty podzieliłeś(-aś)" and Turkish "Ana böldü" but "Sen böldün": one
/// localized sentence serves every actor, so when the actor is this device's user the verb of the "Tú", "Tu", "Du",
/// "Jij", "Ty" or "Sen" is conjugated to the second person. Other languages are left as they are.
///
/// A sentence is localized with the placeholders `actor` and `object` in place of the names, so its verb can be
/// changed before the names, bold or not, go in. The same rules as the web's and Android's `toSecondPerson`.
enum SecondPerson {
    /// Stand-ins for the actor and the object while the sentence is localized: private-use characters no copy has.
    static let actor = "\u{E000}"
    static let object = "\u{E001}"

    private static let irregular = [
        "deshizo": "deshiciste", "cambió": "cambiaste",
        // Why: an -iar verb ends its preterite in "-ió" but takes "-aste", unlike the -er and -ir verbs.
        "envió": "enviaste", "inició": "iniciaste", "copió": "copiaste", "vació": "vaciaste", "anunció": "anunciaste",
    ]

    /// Each language's perfect auxiliary in the third person, with its second person.
    private static let auxiliaries = [
        "fr": ["a": "as"],
        "de": ["hat": "hast", "ist": "bist"],
        "it": ["ha": "hai"],
        "nl": ["heeft": "hebt", "is": "bent"],
    ]

    /// Spanish: "dividió" → "dividiste", "se unió a" → "te uniste a", "cambió la división de" → "cambiaste la división de".
    /// French, German, Italian and Dutch: every verb phrase is a perfect that starts with its auxiliary, so "a divisé" →
    /// "as divisé", "hat geteilt" → "hast geteilt", "ist beigetreten" → "bist beigetreten", "ha diviso" → "hai diviso",
    /// "heeft verdeeld" → "hebt verdeeld", "is lid geworden" → "bent lid geworden".
    /// Polish: every verb phrase starts with a past in "ł(a)", the app not knowing the gender, so "podzielił(a)" →
    /// "podzieliłeś(-aś)". Turkish: every verb phrase ends with its simple past, which takes an "n", so "böldü:" →
    /// "böldün:". Another language keeps the verb as it is.
    static func verbForYou(_ verb: String, language: String? = Bundle.appLanguageCode) -> String {
        switch language {
        case "es": return spanishVerbForYou(verb)
        case "tr": return turkishVerbForYou(verb)
        case "pl": return replacingFirstWord(of: verb) { word in
                word.hasSuffix("ł(a)") ? String(word.dropLast("ł(a)".count)) + "łeś(-aś)" : nil
            }
        case let .some(code) where auxiliaries[code] != nil:
            return replacingFirstWord(of: verb) { auxiliaries[code]?[$0] }
        default: return verb
        }
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
        var end = verb.firstIndex(of: ":") ?? verb.endIndex
        while end > verb.startIndex, verb[verb.index(before: end)].isWhitespace {
            end = verb.index(before: end)
        }
        let clause = verb[..<end]
        guard clause.range(of: "[dt][ıiuü]$", options: .regularExpression) != nil else { return verb }
        return String(clause) + "n" + String(verb[end...])
    }

    private static func spanishVerbForYou(_ verb: String) -> String {
        var words = verb.split(separator: " ", omittingEmptySubsequences: false).map(String.init)
        guard let first = words.first else { return verb }
        if first == "se", words.count > 1 {
            words[1] = spanishVerbForYou(words[1])
            words[0] = "te"
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
