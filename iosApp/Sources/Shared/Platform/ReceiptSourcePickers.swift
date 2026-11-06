import Shared
import SwiftUI

/// "Scan receipt"'s request as a screen's shared code holds it (beta feedback, part 2, item 7; part 3, item 16):
/// Split a bill's own and Home's shortcut ask the same way. The values are read in the screen's body, so a change
/// redraws it; `current` reads the ViewModel itself, for a binding that closes a beat later.
struct ReceiptSourceRequest {
    let showsChoice: Bool
    let shownSource: ReceiptPhotoSource?
    let current: () -> (showsChoice: Bool, shownSource: ReceiptPhotoSource?)
    let pick: (ReceiptPhotoSource) -> Void
    /// The device has no camera to choose: Photos opens without asking.
    let skipChoice: () -> Void
    /// The choice, the camera, Photos or the file panel closed without a photo.
    let dismiss: () -> Void
    /// The photo as it came; nil for one that can't be read, which reads as a receipt that couldn't be read.
    let photoPicked: (Data?) -> Void
    /// The camera's flash, with where it ended up, and "Open Settings": the shared code counts them.
    let torchToggled: (Bool) -> Void
    let settingsOpened: () -> Void

    /// The choice's dialog. Closing it without a pick says so a beat later: a button of the dialog may close it
    /// before its own action has picked.
    func choiceShown(isActive: Bool) -> Binding<Bool> {
        Binding(
            get: { isActive && showsChoice },
            set: { shown in
                guard !shown else { return }
                DispatchQueue.main.async {
                    if current().showsChoice { dismiss() }
                }
            },
        )
    }

    /// The camera, Photos or the file panel, once picked on that choice; closing it without a photo says so.
    func sourceShown(_ source: ReceiptPhotoSource, isActive: Bool) -> Binding<Bool> {
        Binding(
            get: { isActive && shownSource == source },
            set: { shown in
                if !shown, current().shownSource == source { dismiss() }
            },
        )
    }
}

extension View {
    /// "Scan receipt"'s choice: "Take photo" (where there is a camera), "Choose from Photos" and "Choose File…", as
    /// a bill's "Add" offers them; the Mac has no camera to frame it with.
    func receiptSourceDialog(isPresented: Binding<Bool>, pick: @escaping (ReceiptPhotoSource) -> Void) -> some View {
        confirmationDialog(receiptSourceTitle, isPresented: isPresented, titleVisibility: .visible) {
            #if os(iOS)
            Button("Take photo") { pick(.camera) }
            Button("Choose from Photos") { pick(.photos) }
            Button("Choose File…") { pick(.files) }
            #else
            Button("Choose File…") { pick(.files) }
            Button("Choose from Photos") { pick(.photos) }
            #endif
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Fills in the amount, date and description from the photo.")
        }
    }

    /// The request's side of a screen: the dialog (unless a "Scan receipt" button holds it), straight to Photos where
    /// there is no camera to choose, then Photos, the file panel or the receipt camera once picked. `isActive` holds
    /// them back until the screen can present them (Split a bill's sheet still settling).
    func receiptSourcePickers(
        _ request: ReceiptSourceRequest,
        isActive: Bool = true,
        showsDialog: Bool = true,
    ) -> some View {
        modifier(ReceiptSourcePickersModifier(request: request, isActive: isActive, showsDialog: showsDialog))
    }

    private var receiptSourceTitle: Text {
        #if os(macOS)
        Text("Scan Receipt")
        #else
        Text("Scan receipt")
        #endif
    }
}

/// The photo reaches the shared code only once what picked it has closed: Home's photo opens Split a bill's sheet,
/// which UIKit refuses to present while the camera's cover or the Photos sheet is still on its way out.
private struct ReceiptSourcePickersModifier: ViewModifier {
    let request: ReceiptSourceRequest
    let isActive: Bool
    let showsDialog: Bool

    /// The camera's photo, handed over once its cover has closed.
    @State private var takenPhoto: Data?

    /// How long a sheet takes to slide away.
    private static let settle: Duration = .milliseconds(500)

    func body(content: Content) -> some View {
        content
            .receiptSourceDialog(
                isPresented: request.choiceShown(isActive: isActive && ReceiptSource.hasChoice && showsDialog),
                pick: request.pick,
            )
            .onChange(of: isActive && request.showsChoice, initial: true) { _, shows in
                if shows, !ReceiptSource.hasChoice { request.skipChoice() }
            }
            .photoLibraryPicker(isPresented: request.sourceShown(.photos, isActive: isActive)) { data in
                handOverSettled(data)
            }
            .fileImporter(
                isPresented: request.sourceShown(.files, isActive: isActive),
                allowedContentTypes: [.image],
            ) { result in
                guard case let .success(url) = result else { return }
                Task { handOverSettled(await ReceiptSource.readFile(url)) }
            }
            #if os(iOS)
            .fullScreenCover(isPresented: request.sourceShown(.camera, isActive: isActive), onDismiss: handOverTaken) {
                ReceiptCameraView(
                    onPhoto: { data in
                        takenPhoto = data
                        request.dismiss()
                    },
                    onDismiss: request.dismiss,
                    onTorchToggled: request.torchToggled,
                    onSettingsOpened: request.settingsOpened,
                )
            }
            #endif
    }

    private func handOverTaken() {
        guard let photo = takenPhoto else { return }
        takenPhoto = nil
        request.photoPicked(photo)
    }

    /// Photos and the file panel have no "closed" to wait for, so their photo waits for the sheet to slide away.
    private func handOverSettled(_ data: Data?) {
        Task {
            try? await Task.sleep(for: Self.settle)
            request.photoPicked(data)
        }
    }
}

enum ReceiptSource {
    /// A device without a camera (or one restricted) has nothing to choose but Photos, which opens at once.
    static var hasChoice: Bool {
        #if os(iOS)
        CameraPicker.isAvailable
        #else
        true
        #endif
    }

    /// The picked file's bytes, read off the main actor since a large photo or one on iCloud Drive takes a while
    /// (@concurrent: with approachable concurrency a nonisolated async function runs on its caller's actor).
    @concurrent
    nonisolated static func readFile(_ url: URL) async -> Data? {
        let isScoped = url.startAccessingSecurityScopedResource()
        defer { if isScoped { url.stopAccessingSecurityScopedResource() } }
        return try? Data(contentsOf: url)
    }
}
