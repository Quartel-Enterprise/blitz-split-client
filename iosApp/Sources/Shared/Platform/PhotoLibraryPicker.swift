import PhotosUI
import SwiftUI

/// "Choose photo": the system photo picker, which needs no photo library permission and returns one image, or nothing.
private struct PhotoLibraryPicker: ViewModifier {
    @Binding var isPresented: Bool
    let onPicked: (Data?) -> Void

    @State private var picked: PhotosPickerItem?

    func body(content: Content) -> some View {
        content
            .photosPicker(isPresented: $isPresented, selection: $picked, matching: .images)
            .onChange(of: picked) { _, item in
                guard let item else { return }
                picked = nil
                Task { onPicked(try? await item.loadTransferable(type: Data.self)) }
            }
    }
}

extension View {
    /// Hands back the picked image's bytes as the file came (JPEG, PNG, HEIC…), or nil when they can't be read, as a
    /// cloud photo not downloaded.
    func photoLibraryPicker(isPresented: Binding<Bool>, onPicked: @escaping (Data?) -> Void) -> some View {
        modifier(PhotoLibraryPicker(isPresented: isPresented, onPicked: onPicked))
    }
}
