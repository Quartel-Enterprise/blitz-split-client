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

    @State private var scanner = CameraQRScanner(readsContinuously: true)

    var body: some View {
        CameraPreview(session: scanner.session)
            .task {
                onTorchAvailabilityChanged(false)
                guard AVCaptureDevice.default(for: .video) != nil else {
                    onUnavailable()
                    return
                }
                await scanner.start(onScanned: onScanned)
            }
            .onDisappear { scanner.stop() }
    }
}
