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
    /// on iPad, never more than the window under it. Two columns inside follow the sheet's own width
    /// (`ScreenLayout.usesTwoColumns`), so check the sheet on a device when adopting it.
    func billSheetSizing(in window: ScreenLayout) -> some View {
        let isLyingDown = window.size.width > window.size.height
        let size = isLyingDown ? CGSize(width: 820, height: 640) : CGSize(width: 620, height: 860)
        return frame(
            idealWidth: min(size.width, window.size.width),
            idealHeight: min(size.height, window.size.height),
        )
        .presentationSizing(.fitted)
    }
}
