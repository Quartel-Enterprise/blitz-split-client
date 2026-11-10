import SwiftUI

extension View {
    /// A name, a description or a comment starts with a capital letter (beta feedback 3 item 27): its field reads
    /// `binding.firstLetterCapitalized` and sets this. The iPhone's and the iPad's keyboard capitalizes each
    /// sentence, or each word of a person's name ("Maria Silva"); the Mac's keyboard doesn't, so there the binding
    /// uppercases the first letter as it is typed. Never on an email, a payment key, a code, a link, an amount or a
    /// search.
    func capitalizesFirstLetter(isPersonName: Bool = false) -> some View {
        #if os(iOS)
            textInputAutocapitalization(isPersonName ? .words : .sentences)
        #else
            self
        #endif
    }
}

extension Binding<String> {
    /// On the Mac, the field's binding with the first letter typed uppercased; the binding itself on the iPhone and
    /// the iPad. Only what the field sets goes through it, never a text the ViewModel loads (a comment to edit).
    var firstLetterCapitalized: Binding<String> {
        #if os(macOS)
            Binding(
                get: { wrappedValue },
                set: { typed in wrappedValue = FirstLetter.capitalized(typed, after: wrappedValue) ?? typed },
            )
        #else
            self
        #endif
    }
}

enum FirstLetter {
    /// The typed text with its letter uppercased, or nil to leave it as typed: a lone lowercase letter typed into an
    /// empty field, after a dead key's accent, or over a selected text. Not the capital lowercased, nor a lowercase
    /// letter the field had before (deleting down to it), so someone who wants it lowercase keeps it. The caret stays
    /// after it.
    static func capitalized(_ typed: String, after previous: String) -> String? {
        guard typed.count == 1, typed.first?.isLowercase == true, !previous.contains(typed) else { return nil }
        // Why: in the app's language, so Turkish's "i" becomes "İ", not the capital of its dotless "ı"
        let uppercased = typed.uppercased(with: Locale(identifier: SecondPerson.language ?? "en"))
        // "ß" uppercases to "SS": two letters where one was typed
        return uppercased.count == 1 && previous != uppercased ? uppercased : nil
    }
}
