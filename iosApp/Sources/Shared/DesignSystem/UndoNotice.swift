import SwiftUI

/// A message with "Undo" in a Liquid Glass capsule at the bottom, the snackbar of the canvas: shown while `text` is
/// set, and gone `duration` later, when `onExpire` tells the ViewModel it was shown. A new `id` starts it again.
private struct UndoNotice: ViewModifier {
    let id: String?
    let text: String?
    let duration: Duration
    let onUndo: () -> Void
    let onExpire: () -> Void

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let text {
                    HStack(spacing: 12) {
                        Text(verbatim: text)
                            .font(.subheadline.weight(.semibold))
                            .fixedSize(horizontal: false, vertical: true)
                        Button("Undo", action: onUndo)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Palette.accent)
                            .buttonStyle(.plain)
                    }
                    .padding(.leading, 18)
                    .padding(.trailing, 16)
                    .padding(.vertical, 12)
                    .glassEffect(.regular.interactive(), in: .capsule)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .accessibilityElement(children: .contain)
                }
            }
            .animation(.default, value: id)
            .task(id: id) {
                guard id != nil else { return }
                try? await Task.sleep(for: duration)
                guard !Task.isCancelled else { return }
                onExpire()
            }
    }
}

extension View {
    /// Shows `text` with "Undo" while it is set; `onExpire` once it went away without "Undo".
    func undoNotice(
        id: String?,
        text: String?,
        duration: Duration = .seconds(6),
        onUndo: @escaping () -> Void,
        onExpire: @escaping () -> Void,
    ) -> some View {
        modifier(UndoNotice(id: id, text: text, duration: duration, onUndo: onUndo, onExpire: onExpire))
    }
}
