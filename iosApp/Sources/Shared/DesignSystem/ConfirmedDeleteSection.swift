import SwiftUI

/// A form's destructive row ("Delete key", "Delete payment method") whose tap asks first, the question anchored to
/// the row so the iPad shows it as a popover over it. The ViewModel says whether the question shows; `onClosed` runs
/// as the dialog closes itself, after any of its buttons.
struct ConfirmedDeleteSection: View {
    let label: Text
    let question: Text
    let message: Text
    let isDisabled: Bool
    let isConfirmationShown: Bool
    let onDelete: () -> Void
    let onConfirm: () -> Void
    let onClosed: () -> Void

    var body: some View {
        Section {
            Button(role: .destructive, action: onDelete) {
                label.destructiveRowLabel()
            }
            .disabled(isDisabled)
            .confirmationDialog(
                question,
                isPresented: Binding(
                    get: { isConfirmationShown },
                    set: { isPresented in
                        if !isPresented { onClosed() }
                    },
                ),
                titleVisibility: .visible,
            ) {
                Button("Delete", role: .destructive, action: onConfirm)
                Button("Cancel", role: .cancel) {}
            } message: {
                message
            }
        }
    }
}
