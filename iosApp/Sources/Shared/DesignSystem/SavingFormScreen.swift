import SwiftUI

/// A form of the profile that saves one thing (a key, a payment method): its fields shimmer in place until read, Save
/// in the bar and, as a sheet, Cancel beside it; the sheet can't be swiped away while it saves.
struct SavingFormScreen<Content: View>: View {
    let title: Text
    @Binding var layout: ScreenLayout
    let isSheet: Bool
    let canSave: Bool
    let isSaving: Bool
    let onCancel: () -> Void
    let onSave: () -> Void
    /// Whether the thing to edit was read: until then its fields shimmer.
    let isLoaded: Bool
    /// The form, drawn once read.
    @ViewBuilder let content: () -> Content

    var body: some View {
        Group {
            if isLoaded {
                content()
            } else {
                ShimmerScreen {
                    ShimmerForm(fieldCount: 3)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        .measuringScreenLayout($layout)
        .navigationTitle(title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            if isSheet {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", action: onSave)
                    .disabled(!canSave)
            }
        }
        .interactiveDismissDisabled(isSaving)
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 500, minHeight: 400, idealHeight: 560)
        #endif
    }
}
