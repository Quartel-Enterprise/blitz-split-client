import SwiftUI

/// A number field whose ViewModel may refuse part of what is typed (a letter from an iPad's or a Mac's keyboard, a
/// third decimal), showing what the ViewModel kept. Bound straight to the ViewModel, a TextField would keep showing
/// the refused text, since the text it reads back didn't change.
struct KeptTextField<Label: View>: View {
    @Binding var text: String
    let prompt: Text?
    /// Whether the field holds a number, whose digits run left to right in Arabic and Hebrew too.
    let isNumber: Bool
    @ViewBuilder let label: () -> Label

    @State private var shown = ""
    @Environment(\.layoutDirection) private var layoutDirection

    init(
        text: Binding<String>,
        prompt: Text? = nil,
        isNumber: Bool = true,
        @ViewBuilder label: @escaping () -> Label,
    ) {
        _text = text
        self.prompt = prompt
        self.isNumber = isNumber
        self.label = label
    }

    var body: some View {
        TextField(text: $shown, prompt: prompt, label: label)
            .onChange(of: shown) { _, typed in
                // The field handing back the text it was given is no edit
                guard typed != text else { return }
                text = typed
                // Why: an emptied field stays empty while the ViewModel keeps its value until a number comes (the
                // Mac's Quantity), else the old number would come back and the next digit land after it
                if text != typed, !typed.isEmpty { shown = text }
            }
            .onChange(of: text, initial: true) {
                // Why: the ViewModel's text now, not the change's: a quick keystroke may already have moved past it
                if text != shown { shown = text }
            }
            // Why: in right to left a number typed so far ("12.") would read ".12"; the field keeps its own place
            .environment(\.layoutDirection, isNumber ? .leftToRight : layoutDirection)
    }
}

extension KeptTextField where Label == Text {
    init(_ title: LocalizedStringKey, text: Binding<String>) {
        self.init(text: text) { Text(title) }
    }
}
