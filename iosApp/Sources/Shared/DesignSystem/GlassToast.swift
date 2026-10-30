import SwiftUI

/// A short message in a Liquid Glass capsule at the top ("Coming soon"), shown each time `trigger` grows and
/// hidden two seconds after the last one.
private struct GlassToast: ViewModifier {
    let text: LocalizedStringKey
    let trigger: Int

    @State private var isShown = false
    @State private var hides: Task<Void, Never>?

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) {
                if isShown {
                    Text(text)
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .glassEffect(.regular, in: .capsule)
                        .padding(.top, 8)
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .accessibilityAddTraits(.isStaticText)
                }
            }
            .onChange(of: trigger) {
                withAnimation { isShown = true }
                hides?.cancel()
                hides = Task {
                    try? await Task.sleep(for: .seconds(2))
                    guard !Task.isCancelled else { return }
                    withAnimation { isShown = false }
                }
            }
    }
}

extension View {
    /// Shows `text` for two seconds whenever `trigger` changes: a model counts each action that asks for it.
    func glassToast(_ text: LocalizedStringKey, trigger: Int) -> some View {
        modifier(GlassToast(text: text, trigger: trigger))
    }
}
