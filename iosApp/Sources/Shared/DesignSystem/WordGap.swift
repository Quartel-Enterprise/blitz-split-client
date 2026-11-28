import Foundation

/// What goes between the pieces of a sentence the code joins (two sentences of a detail): a space, but nothing in
/// Japanese and Chinese, which write them with none and end a sentence with "。", not ".". Hindi, Bengali, Odia, Nepali and
/// Punjabi end one with their danda, "।", Urdu with "۔", Burmese with "။",
/// Khmer with "។", Amharic with "።", and Thai with nothing, the space between two being its stop. The same rule as the web's `wordGap` and
/// Android's `wordGapUi`.
enum WordGap {
    private static var isUnspaced: Bool { ["ja", "zh"].contains(Bundle.appLanguageCode) }

    static var gap: String { isUnspaced ? "" : " " }

    /// Every language's sentence stop, so a piece that already ends with one doesn't get a second.
    static let stops = [".", "。", "।", "۔", "။", "។", "።"]

    static var fullStop: String {
        if isUnspaced { return "。" }
        switch Bundle.appLanguageCode {
        case "hi", "bn", "or", "ne", "pa": return "।"
        case "ur": return "۔"
        case "my": return "။"
        case "km": return "។"
        case "am": return "።"
        case "th": return ""
        default: return "."
        }
    }
}
