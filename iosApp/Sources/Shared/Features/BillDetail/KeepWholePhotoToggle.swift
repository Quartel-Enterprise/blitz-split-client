import Shared
import SwiftUI

/// "Show the whole photo when opened" in "Adjust photo", only for a bill's photo (beta feedback of 2026-11-25): on, the
/// viewer shows the whole photo and the tile the square framed; off, both show the square. White on the editor's dark,
/// as its other controls. It reads the model, so it follows the switch while the editor is open.
struct KeepWholePhotoToggle: View {
    let model: BillDetailModel

    var body: some View {
        Toggle(
            "Show the whole photo when opened",
            isOn: Binding(
                get: { model.state.keepsWholePhoto },
                set: { model.send(BillDetailUiEventOnKeepWholePhotoChanged(isOn: $0)) },
            ),
        )
        // Why: a switch on the Mac too, whose default style is a checkbox.
        .toggleStyle(.switch)
        .font(.subheadline)
        .foregroundStyle(.white)
        .tint(.white)
    }
}
