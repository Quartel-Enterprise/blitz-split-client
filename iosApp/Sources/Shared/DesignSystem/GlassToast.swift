import SwiftUI

/// One message a screen can show as a toast, and the count its model grows on each action that asks for it.
struct GlassToastMessage {
    let text: LocalizedStringKey
    let trigger: Int
}

/// A short message in a Liquid Glass capsule at the top ("Coming soon"), shown each time one of the triggers grows
/// and hidden `duration` after the last one. A second message replaces the one on screen.
private struct GlassToast: ViewModifier {
    let messages: [GlassToastMessage]
    let duration: Duration

    @State private var shown: LocalizedStringKey?
    @State private var hides: Task<Void, Never>?

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) {
                if let shown {
                    Text(shown)
                        .font(.subheadline.weight(.semibold))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .glassEffect(.regular, in: .capsule)
                        .padding(.top, 8)
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .accessibilityAddTraits(.isStaticText)
                }
            }
            .onChange(of: messages.map(\.trigger)) { old, new in
                guard let index = new.indices.first(where: { old.indices.contains($0) && old[$0] != new[$0] })
                else { return }
                show(messages[index].text)
            }
    }

    private func show(_ text: LocalizedStringKey) {
        withAnimation { shown = text }
        hides?.cancel()
        hides = Task {
            try? await Task.sleep(for: duration)
            guard !Task.isCancelled else { return }
            withAnimation { shown = nil }
        }
    }
}

extension View {
    /// Shows `text` for `duration` whenever `trigger` changes: a model counts each action that asks for it.
    func glassToast(_ text: LocalizedStringKey, trigger: Int, duration: Duration = .seconds(2)) -> some View {
        glassToast([GlassToastMessage(text: text, trigger: trigger)], duration: duration)
    }

    /// One toast for several messages, so they replace each other instead of stacking.
    func glassToast(_ messages: [GlassToastMessage], duration: Duration = .seconds(2)) -> some View {
        modifier(GlassToast(messages: messages, duration: duration))
    }
}
