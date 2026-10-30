@preconcurrency import AVFoundation
import SwiftUI
import Vision

/// Scans the old device's recovery QR with the Mac's camera ("Scan the QR code with the camera").
struct QRScannerView: View {
    let onScanned: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var scanner = CameraQRScanner()

    var body: some View {
        VStack(spacing: 16) {
            Text("Scan the QR code with the camera").font(.headline)
            Group {
                if scanner.isAuthorized {
                    CameraPreview(session: scanner.session)
                        .clipShape(.rect(cornerRadius: 16))
                } else {
                    ContentUnavailableView(
                        "Camera unavailable",
                        systemImage: "camera.badge.ellipsis",
                        description: Text("Allow camera access in System Settings or type the code."),
                    )
                }
            }
            .frame(width: 420, height: 315)
            Text("On the old device: Profile › Recovery › Show QR code")
                .font(.footnote)
                .foregroundStyle(.secondary)
            HStack {
                Spacer()
                Button("Cancel", role: .cancel) { dismiss() }
                    .keyboardShortcut(.cancelAction)
            }
        }
        .padding(20)
        .task {
            await scanner.start { content in
                onScanned(content)
                dismiss()
            }
        }
        .onDisappear { scanner.stop() }
    }
}

/// Feeds camera frames to Vision's barcode detector and reports the first QR it reads, or every one it reads while it
/// reads continuously (the invite reader, which shows a wrong QR's warning and keeps reading).
@Observable
final class CameraQRScanner: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private(set) var isAuthorized = false
    @ObservationIgnored let session = AVCaptureSession()
    @ObservationIgnored private let readsContinuously: Bool
    @ObservationIgnored private let queue = DispatchQueue(label: "com.quare.blitzsplit.qr")
    @ObservationIgnored private var onScanned: ((String) -> Void)?
    @ObservationIgnored nonisolated(unsafe) private var didScan = false
    /// The camera sees the same QR on every frame, so reading continuously reports it a few times a second, not 30.
    @ObservationIgnored nonisolated(unsafe) private var lastRead: (content: String, at: ContinuousClock.Instant)?

    init(readsContinuously: Bool = false) {
        self.readsContinuously = readsContinuously
    }

    func start(onScanned: @escaping (String) -> Void) async {
        self.onScanned = onScanned
        isAuthorized = await AVCaptureDevice.requestAccess(for: .video)
        guard isAuthorized,
              let camera = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: camera) else { return }
        let output = AVCaptureVideoDataOutput()
        output.setSampleBufferDelegate(self, queue: queue)
        session.beginConfiguration()
        if session.canAddInput(input) { session.addInput(input) }
        if session.canAddOutput(output) { session.addOutput(output) }
        session.commitConfiguration()
        let session = session
        queue.async { session.startRunning() }
    }

    func stop() {
        let session = session
        queue.async { session.stopRunning() }
    }

    nonisolated func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection,
    ) {
        guard !didScan, let pixels = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let request = VNDetectBarcodesRequest()
        request.symbologies = [.qr]
        try? VNImageRequestHandler(cvPixelBuffer: pixels).perform([request])
        guard let content = request.results?.first?.payloadStringValue else { return }
        let now = ContinuousClock.now
        if let lastRead, lastRead.content == content, now - lastRead.at < .milliseconds(250) { return }
        lastRead = (content, now)
        didScan = !readsContinuously
        Task { @MainActor in
            self.onScanned?(content)
        }
    }
}

struct CameraPreview: NSViewRepresentable {
    let session: AVCaptureSession

    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        view.layer = layer
        view.wantsLayer = true
        return view
    }

    func updateNSView(_ view: NSView, context: Context) {}
}
