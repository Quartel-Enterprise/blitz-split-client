import SwiftUI

/// Spanish says "Ana dividió" but "Tú dividiste": one localized sentence serves every actor, so when the actor is this
/// device's user the verb after the "Tú" is conjugated to the second person. Other languages are left as they are.
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

    static var isSpanish: Bool {
        Bundle.main.preferredLocalizations.first?.hasPrefix("es") == true
    }

    /// "dividió" → "dividiste", "se unió a" → "te uniste a", "cambió la división de" → "cambiaste la división de".
    static func verbForYou(_ verb: String, isSpanish: Bool = SecondPerson.isSpanish) -> String {
        guard isSpanish else { return verb }
        var words = verb.split(separator: " ", omittingEmptySubsequences: false).map(String.init)
        guard let first = words.first else { return verb }
        if first == "se", words.count > 1 {
            words[1] = verbForYou(words[1], isSpanish: true)
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

    /// The localized sentence with the verb right after the actor said to "Tú" when the actor is the user, in Spanish.
    static func template(_ template: String, actorIsMe: Bool) -> String {
        guard actorIsMe, isSpanish, template.hasPrefix(actor + " ") else { return template }
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
