@preconcurrency import AVFoundation
import SwiftUI
import VisionKit

/// The invite reader's live camera: VisionKit's scanner, reporting every QR it sees while it keeps reading, so a wrong
/// one shows its warning and the next one still opens. The screen draws its own viewfinder over it.
struct InviteScannerView: View {
    let isTorchOn: Bool
    let onScanned: (String) -> Void
    let onTorchAvailabilityChanged: (Bool) -> Void
    let onUnavailable: () -> Void

    var body: some View {
        Group {
            if DataScannerViewController.isSupported, DataScannerViewController.isAvailable {
                ScannerRepresentable(onScanned: onScanned)
            } else {
                Color.black
            }
        }
        .ignoresSafeArea()
        .onAppear {
            // Why: the scanner needs a recent enough camera; without one, only the paste is left.
            if !DataScannerViewController.isSupported { onUnavailable() }
            onTorchAvailabilityChanged(Torch.device?.hasTorch ?? false)
        }
        .onChange(of: isTorchOn, initial: true) { _, isOn in Torch.set(isOn) }
        .onDisappear { Torch.set(false) }
    }
}

/// The back camera's flashlight, which VisionKit leaves to the app while its scanner runs.
private enum Torch {
    static var device: AVCaptureDevice? {
        AVCaptureDevice.default(for: .video)
    }

    static func set(_ isOn: Bool) {
        guard let device, device.hasTorch, (try? device.lockForConfiguration()) != nil else { return }
        device.torchMode = isOn ? .on : .off
        device.unlockForConfiguration()
    }
}

private struct ScannerRepresentable: UIViewControllerRepresentable {
    let onScanned: (String) -> Void

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.qr])],
            qualityLevel: .fast,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: false,
        )
        scanner.delegate = context.coordinator
        try? scanner.startScanning()
        return scanner
    }

    func updateUIViewController(_ scanner: DataScannerViewController, context: Context) {
        context.coordinator.onScanned = onScanned
    }

    static func dismantleUIViewController(_ scanner: DataScannerViewController, coordinator: Coordinator) {
        scanner.stopScanning()
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onScanned: onScanned)
    }

    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        var onScanned: (String) -> Void
        /// The scanner updates a QR in view on every frame, so it is reported a few times a second, not 30.
        private var lastRead: (content: String, at: ContinuousClock.Instant)?

        init(onScanned: @escaping (String) -> Void) {
            self.onScanned = onScanned
        }

        func dataScanner(
            _ dataScanner: DataScannerViewController,
            didAdd addedItems: [RecognizedItem],
            allItems: [RecognizedItem],
        ) {
            report(addedItems)
        }

        func dataScanner(
            _ dataScanner: DataScannerViewController,
            didUpdate updatedItems: [RecognizedItem],
            allItems: [RecognizedItem],
        ) {
            report(updatedItems)
        }

        private func report(_ items: [RecognizedItem]) {
            for case let .barcode(barcode) in items {
                if let content = barcode.payloadStringValue {
                    let now = ContinuousClock.now
                    if let lastRead, lastRead.content == content, now - lastRead.at < .milliseconds(250) { return }
                    lastRead = (content, now)
                    onScanned(content)
                    return
                }
            }
        }
    }
}
