@preconcurrency import AVFoundation
import SwiftUI

/// The invite reader's live camera on the Mac: every QR it reads is reported while it keeps reading, so a wrong one
/// shows its warning and the next one still opens. A Mac's camera has no flashlight, and a Mac mini or Studio may have
/// no camera at all.
struct InviteScannerView: View {
    let isTorchOn: Bool
    let onScanned: (String) -> Void
    let onTorchAvailabilityChanged: (Bool) -> Void
    let onUnavailable: () -> Void

    /// Made once the camera starts: SwiftUI rebuilds this view's value whenever the reader redraws, and a capture
    /// session made each time would be thrown away.
    @State private var scanner: CameraQRScanner?

    var body: some View {
        Group {
            if let scanner {
                CameraPreview(session: scanner.session)
            } else {
                Color.black
            }
        }
        .task {
            onTorchAvailabilityChanged(false)
            guard AVCaptureDevice.default(for: .video) != nil else {
                onUnavailable()
                return
            }
            let scanner = scanner ?? CameraQRScanner(readsContinuously: true)
            self.scanner = scanner
            await scanner.start(onScanned: onScanned)
            // Why: the camera wouldn't open (another app holds it), so only the paste is left.
            if scanner.session.inputs.isEmpty { onUnavailable() }
        }
        .onDisappear { scanner?.stop() }
    }
}
