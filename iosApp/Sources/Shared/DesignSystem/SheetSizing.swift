import SwiftUI

/// The sizes the canvas draws a sheet at with an iPad's room (an iPad, an iPhone Duo open). An iPhone's sheet stays
/// the system's full width whatever is asked, and the Mac sizes its sheets by their content's frame.
extension View {
    /// A form over the screen behind it (the recovery code, Novo grupo, Editar grupo, Registrar pagamento 540×620,
    /// Cobrar): the system's form sheet, about 540 pt wide on iPad.
    func formSheetSizing() -> some View {
        presentationSizing(.form)
    }

    /// A bill (Rachar conta, Conta): 820×640 where the window lies down and 620×860 upright, as the canvas draws them
    /// with an iPad's room, never more than the window under it; an iPhone's sheet stays as the system draws it. Two
    /// columns inside follow the sheet's own width (`ScreenLayout.usesTwoColumns`): the 820 pt sheet splits, the
    /// 620 pt one doesn't.
    @ViewBuilder
    func billSheetSizing(in window: ScreenLayout) -> some View {
        if window.isLarge {
            let isLyingDown = window.size.width > window.size.height
            let size = isLyingDown ? CGSize(width: 820, height: 640) : CGSize(width: 620, height: 860)
            presentationSizing(
                FixedSheetSizing(
                    size: CGSize(
                        width: min(size.width, window.size.width),
                        height: min(size.height, window.size.height),
                    ),
                ),
            )
        } else {
            self
        }
    }
}

/// A sheet of the size the canvas draws, whatever its content would ask for: a bill's sheet holds a NavigationStack
/// and lists, which have no ideal size of their own to fit.
private struct FixedSheetSizing: PresentationSizing {
    let size: CGSize

    func proposedSize(for root: PresentationSizingRoot, context: PresentationSizingContext) -> ProposedViewSize {
        ProposedViewSize(size)
    }
}
