import SwiftUI

/// The sizes the canvas draws a sheet at with an iPad's room (an iPad, an iPhone Duo open). An iPhone's sheet stays
/// the system's full width whatever is asked, and the Mac sizes its sheets by their content's frame.
extension View {
    /// A form over the screen behind it (the recovery code, Novo grupo, Editar grupo, Registrar pagamento 540×620,
    /// Cobrar): the system's form sheet, about 540 pt wide on iPad.
    func formSheetSizing() -> some View {
        presentationSizing(.form)
            .foldSheetPlacement(isForm: true)
    }

    /// A bill (Rachar conta, Conta) at `ScreenLayout.billSheetSize`; an iPhone's sheet stays as the system draws it.
    /// Two columns inside follow the sheet's own width (`ScreenLayout.usesTwoColumns`): the 820 pt sheet splits, the
    /// 620 pt one doesn't. The root's sheet asks for the same size (`AppRoute.usesBillSheet`), since its own sizing
    /// (`foldSheetPlacement`) is the one iOS 27 keeps.
    @ViewBuilder
    func billSheetSizing(in window: ScreenLayout) -> some View {
        if let size = window.billSheetSize {
            presentationSizing(FixedSheetSizing(size: size))
        } else {
            self
        }
    }
}

extension View {
    /// The buttons at the foot of a sheet shared with iOS: large on iPhone and iPad, where a thumb taps them, and the
    /// regular size a Mac sheet's buttons have.
    func sheetButtonSize() -> some View {
        #if os(macOS)
        controlSize(.regular)
        #else
        controlSize(.large)
        #endif
    }
}

extension ScreenLayout {
    /// A bill's sheet with an iPad's room: 820×640 where the window lies down and 620×860 upright, as the canvas draws
    /// them, never more than the window under it. Nil without an iPad's room.
    var billSheetSize: CGSize? {
        guard isLarge else { return nil }
        let isLyingDown = size.width > size.height
        let bill = isLyingDown ? CGSize(width: 820, height: 640) : CGSize(width: 620, height: 860)
        return CGSize(width: min(bill.width, size.width), height: min(bill.height, size.height))
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
