import SwiftUI

/// A number field whose ViewModel may refuse part of what is typed (a letter from an iPad's or a Mac's keyboard, a
/// third decimal), showing what the ViewModel kept. Bound straight to the ViewModel, a TextField would keep showing
/// the refused text, since the text it reads back didn't change.
struct KeptTextField<Label: View>: View {
    @Binding var text: String
    let prompt: Text?
    @ViewBuilder let label: () -> Label

    @State private var shown = ""

    init(text: Binding<String>, prompt: Text? = nil, @ViewBuilder label: @escaping () -> Label) {
        _text = text
        self.prompt = prompt
        self.label = label
    }

    var body: some View {
        TextField(text: $shown, prompt: prompt, label: label)
            .onChange(of: shown) { _, typed in
                // The field handing back the text it was given is no edit
                guard typed != text else { return }
                text = typed
                if text != typed { shown = text }
            }
            .onChange(of: text, initial: true) {
                // Why: the ViewModel's text now, not the change's: a quick keystroke may already have moved past it
                if text != shown { shown = text }
            }
    }
}

extension KeptTextField where Label == Text {
    init(_ title: LocalizedStringKey, text: Binding<String>) {
        self.init(text: text) { Text(title) }
    }
}
