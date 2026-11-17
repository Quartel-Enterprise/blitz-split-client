import Foundation

/// What goes between the pieces of a sentence the code joins (two sentences of a detail): a space, but nothing in
/// Japanese, which writes them with none and ends a sentence with "。", not ".". Hindi ends one with its danda, "।".
/// The same rule as the web's `wordGap` and Android's `wordGapUi`.
enum WordGap {
    private static var isUnspaced: Bool { Bundle.appLanguageCode == "ja" }

    static var gap: String { isUnspaced ? "" : " " }

    static var fullStop: String {
        if isUnspaced { return "。" }
        return Bundle.appLanguageCode == "hi" ? "।" : "."
    }
}
