import SwiftUI

/// What a custom clickable view (a `.plain` button: a card, a row outside a `List`, a chip, a swatch, a face) does
/// under a pointer. Apple's guidelines keep the arrow on a Mac button, so the view itself answers the hover: a light
/// accent fill in its shape on the Mac, the system's highlight on an iPad's pointer. An iPhone has no pointer, so
/// nothing changes there. Standard controls and the rows of a `List` already answer it themselves; never add it to
/// them. A link to a web page takes `.linkPointer()` as well.
private struct HoverHighlight<S: Shape>: ViewModifier {
    let shape: S

    @Environment(\.isEnabled) private var isEnabled
    @State private var isHovered = false

    func body(content: Content) -> some View {
        #if os(macOS)
        content
            .overlay {
                if isHovered && isEnabled {
                    shape
                        .fill(Palette.accent.opacity(0.08))
                        .allowsHitTesting(false)
                }
            }
            .onHover { isHovered = $0 }
            .animation(.easeOut(duration: 0.15), value: isHovered)
        #else
        content
            .contentShape(.hoverEffect, shape)
            .hoverEffect(.highlight, isEnabled: isEnabled)
        #endif
    }
}

/// The same answer for content with no shape of its own to fill (a face, a photo, a name, a lone icon): it fades a
/// little on the Mac, and the iPad's pointer lifts it.
private struct HoverDim: ViewModifier {
    let isOn: Bool

    @Environment(\.isEnabled) private var isEnabled
    @State private var isHovered = false

    func body(content: Content) -> some View {
        #if os(macOS)
        content
            .opacity(isOn && isHovered && isEnabled ? 0.75 : 1)
            .onHover { isHovered = $0 }
            .animation(.easeOut(duration: 0.15), value: isHovered)
        #else
        content.hoverEffect(.lift, isEnabled: isOn && isEnabled)
        #endif
    }
}

extension AnyShape {
    /// The shape of `hoverRowHighlight()`, for a caller that picks between it and a card's own shape.
    static var hoverRow: AnyShape {
        AnyShape(RoundedRectangle(cornerRadius: 12).inset(by: 5))
    }
}

extension View {
    /// Fills `shape` lightly while a pointer is over this clickable view; on the button, after its `buttonStyle`.
    func hoverHighlight(in shape: some Shape = .rect) -> some View {
        modifier(HoverHighlight(shape: shape))
    }

    /// The highlight of a row: a rounded rect inset from the row's edges, like a Mac menu's, so it never reaches past
    /// the rounded corners of the card the row sits in.
    func hoverRowHighlight() -> some View {
        hoverHighlight(in: AnyShape.hoverRow)
    }

    /// Fades this clickable face, photo, name or icon a little while a pointer is over it.
    func hoverDim(_ isOn: Bool = true) -> some View {
        modifier(HoverDim(isOn: isOn))
    }

    /// The pointing hand over a button that opens a web page: on the Mac, Apple keeps it for links.
    func linkPointer() -> some View {
        #if os(macOS)
        pointerStyle(.link)
        #else
        self
        #endif
    }
}
