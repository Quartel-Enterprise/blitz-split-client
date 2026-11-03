import AVFoundation
import SwiftUI
import UIKit

/// "Ler cupom fiscal" (7.1e): the board's own camera over the bill, black in both appearances, with the frame to fit
/// the receipt in, the torch, Photos and the shutter. The photo taken or picked goes to the shared code as it came; a
/// photo that can't be read goes too, as empty bytes, and reads as a receipt that couldn't be read.
struct ReceiptCameraView: View {
    let onPhoto: (Data) -> Void
    let onDismiss: () -> Void

    @State private var capture = ReceiptCapture()
    @State private var access = CameraAccess.asking
    @State private var hasTorch = false
    @State private var isTorchOn = false
    @State private var isTaking = false
    @State private var showsPhotoPicker = false
    @Environment(\.openURL) private var openURL

    private enum CameraAccess {
        case asking, allowed, denied, unavailable
    }

    var body: some View {
        GeometryReader { proxy in
            let isLandscape = proxy.size.width > proxy.size.height
            Group {
                if isLandscape {
                    HStack(spacing: 0) {
                        VStack(spacing: 0) {
                            header
                            viewfinder
                            hint
                                .padding(.vertical, 12)
                        }
                        controls(isVertical: true)
                            .frame(width: 136)
                    }
                } else {
                    VStack(spacing: 0) {
                        header
                        viewfinder
                        hint
                            .padding(.top, 14)
                        controls(isVertical: false)
                            .frame(height: 150)
                    }
                }
            }
        }
        .background(Color.black.ignoresSafeArea())
        .foregroundStyle(.white)
        .environment(\.colorScheme, .dark)
        .photoLibraryPicker(isPresented: $showsPhotoPicker) { data in onPhoto(data ?? Data()) }
        .task { await open() }
        .onDisappear { capture.stop() }
        .onChange(of: isTorchOn) { _, isOn in capture.setTorch(isOn) }
    }

    private var header: some View {
        HStack(spacing: 10) {
            CameraCircleButton(systemImage: "xmark", label: "Cancel", action: onDismiss)
            Text("Scan receipt")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .accessibilityAddTraits(.isHeader)
            if hasTorch {
                CameraCircleButton(
                    systemImage: isTorchOn ? "bolt.slash.fill" : "bolt.fill",
                    label: isTorchOn ? "Turn off the flash" : "Turn on the flash",
                ) { isTorchOn.toggle() }
            } else {
                Color.clear.frame(width: 44, height: 44)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    @ViewBuilder
    private var viewfinder: some View {
        ZStack {
            switch access {
            case .allowed:
                ReceiptCameraPreview(capture: capture)
                RoundedRectangle(cornerRadius: 26)
                    .strokeBorder(.white.opacity(0.9), lineWidth: 3)
                    .padding(.horizontal, 64)
                    .padding(.vertical, 30)
                    .accessibilityHidden(true)
            case .asking:
                Color.clear
            case .denied:
                CameraMessage(text: "Allow the camera to scan the receipt, or choose a photo of it from Photos.") {
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                    .buttonStyle(.glass)
                }
            case .unavailable:
                CameraMessage(text: "The camera isn't available. Choose a photo of the receipt from Photos.") {
                    EmptyView()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    private var hint: some View {
        Text("Frame the whole receipt. We fill in the amount, the date and the description; you check them before saving.")
            .font(.subheadline)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 32)
            .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private func controls(isVertical: Bool) -> some View {
        let layout = isVertical
            ? AnyLayout(VStackLayout(spacing: 0))
            : AnyLayout(HStackLayout(spacing: 0))
        layout {
            if isVertical {
                Color.clear.frame(width: 44, height: 44)
            } else {
                photosButton
            }
            Spacer(minLength: 0)
            shutter
            Spacer(minLength: 0)
            if isVertical {
                photosButton
            } else {
                Color.clear.frame(width: 44, height: 44)
            }
        }
        .padding(isVertical ? .vertical : .horizontal, 40)
    }

    private var photosButton: some View {
        CameraCircleButton(
            systemImage: "photo.on.rectangle",
            label: "Choose from Photos",
            cornerRadius: 12,
        ) { showsPhotoPicker = true }
    }

    private var shutter: some View {
        let canTake = access == .allowed && !isTaking
        return Button {
            isTaking = true
            capture.takePhoto { data in
                isTaking = false
                // A failed capture reads as a photo that couldn't be read, which offers "Tirar outra foto".
                onPhoto(data ?? Data())
            }
        } label: {
            Circle()
                .strokeBorder(.white, lineWidth: 4)
                .frame(width: 76, height: 76)
                .overlay {
                    Circle()
                        .fill(.white.opacity(canTake ? 1 : 0.38))
                        .frame(width: 60, height: 60)
                }
        }
        .buttonStyle(.plain)
        .disabled(!canTake)
        .accessibilityLabel(Text("Take photo"))
    }

    /// Asks for the camera first, as the system camera shows a black screen once refused, then starts it.
    private func open() async {
        guard AVCaptureDevice.default(for: .video) != nil else {
            access = .unavailable
            return
        }
        let isAllowed = switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: true
        case .notDetermined: await AVCaptureDevice.requestAccess(for: .video)
        default: false
        }
        guard isAllowed else {
            access = .denied
            return
        }
        // The picture shows once the session has its camera, so the preview's rotation has a connection to turn
        capture.start { torch in
            guard let torch else {
                access = .unavailable
                return
            }
            hasTorch = torch
            access = .allowed
        }
    }
}

private struct CameraCircleButton: View {
    let systemImage: String
    let label: LocalizedStringKey
    var cornerRadius: CGFloat = 22
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.body.weight(.semibold))
                .frame(width: 44, height: 44)
                .background(.white.opacity(0.18), in: .rect(cornerRadius: cornerRadius))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label))
    }
}

private struct CameraMessage<Action: View>: View {
    let text: LocalizedStringKey
    @ViewBuilder let action: () -> Action

    var body: some View {
        VStack(spacing: 16) {
            Text(text)
                .multilineTextAlignment(.center)
            action()
        }
        .padding(.horizontal, 32)
    }
}

/// The back camera's session, on a queue of its own since starting and stopping it block.
@MainActor
@Observable
final class ReceiptCapture {
    @ObservationIgnored private let worker = ReceiptCaptureWorker()
    /// Turns the preview and the photo upright however the device is held.
    @ObservationIgnored var rotation: AVCaptureDevice.RotationCoordinator?

    var session: AVCaptureSession {
        worker.session
    }

    /// Reports whether the camera has a torch once it runs, or nil when it can't start.
    func start(onStarted: @escaping @MainActor (Bool?) -> Void) {
        worker.start { hasTorch in
            Task { @MainActor in onStarted(hasTorch) }
        }
    }

    func stop() {
        worker.stop()
    }

    func setTorch(_ isOn: Bool) {
        worker.setTorch(isOn)
    }

    func takePhoto(onTaken: @escaping @MainActor (Data?) -> Void) {
        worker.takePhoto(angle: rotation?.videoRotationAngleForHorizonLevelCapture) { data in
            Task { @MainActor in onTaken(data) }
        }
    }
}

/// What runs off the main thread: the session's setup, the torch and the photo's capture, all on one serial queue.
nonisolated private final class ReceiptCaptureWorker: NSObject, AVCapturePhotoCaptureDelegate, @unchecked Sendable {
    let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "com.quare.blitzsplit.receipt-camera")
    private let output = AVCapturePhotoOutput()
    private var device: AVCaptureDevice?
    private var onTaken: (@Sendable (Data?) -> Void)?

    /// Reports whether the camera has a torch once it runs, or nil when it can't start.
    func start(onStarted: @escaping @Sendable (Bool?) -> Void) {
        queue.async { [self] in
            guard
                let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
                    ?? AVCaptureDevice.default(for: .video),
                let input = try? AVCaptureDeviceInput(device: camera)
            else {
                onStarted(nil)
                return
            }
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard session.canAddInput(input), session.canAddOutput(output) else {
                session.commitConfiguration()
                onStarted(nil)
                return
            }
            session.addInput(input)
            session.addOutput(output)
            // A receipt's small print reads better sharp than fast
            output.maxPhotoQualityPrioritization = .quality
            session.commitConfiguration()
            session.startRunning()
            device = camera
            onStarted(session.isRunning ? camera.hasTorch : nil)
        }
    }

    func stop() {
        queue.async { [self] in session.stopRunning() }
    }

    func setTorch(_ isOn: Bool) {
        queue.async { [self] in
            guard let device, device.hasTorch, (try? device.lockForConfiguration()) != nil else { return }
            device.torchMode = isOn ? .on : .off
            device.unlockForConfiguration()
        }
    }

    func takePhoto(angle: CGFloat?, onTaken: @escaping @Sendable (Data?) -> Void) {
        queue.async { [self] in
            guard self.onTaken == nil, device != nil else {
                onTaken(nil)
                return
            }
            if let angle, let connection = output.connection(with: .video), connection.isVideoRotationAngleSupported(angle) {
                connection.videoRotationAngle = angle
            }
            let settings = AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])
            settings.photoQualityPrioritization = .quality
            self.onTaken = onTaken
            output.capturePhoto(with: settings, delegate: self)
        }
    }

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: (any Error)?) {
        let data = error == nil ? photo.fileDataRepresentation() : nil
        queue.async { [self] in
            let callback = onTaken
            onTaken = nil
            callback?(data)
        }
    }
}

/// The camera's picture, filling the area, kept upright by the device's rotation coordinator.
private struct ReceiptCameraPreview: UIViewRepresentable {
    let capture: ReceiptCapture

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = capture.session
        view.previewLayer.videoGravity = .resizeAspectFill
        view.onRotation = { coordinator in capture.rotation = coordinator }
        return view
    }

    func updateUIView(_ view: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override static var layerClass: AnyClass {
            AVCaptureVideoPreviewLayer.self
        }

        var previewLayer: AVCaptureVideoPreviewLayer {
            layer as! AVCaptureVideoPreviewLayer
        }

        var onRotation: ((AVCaptureDevice.RotationCoordinator) -> Void)?
        private var rotation: AVCaptureDevice.RotationCoordinator?
        private var observation: NSKeyValueObservation?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            guard window != nil, rotation == nil,
                  let device = (previewLayer.session?.inputs.first as? AVCaptureDeviceInput)?.device
                      ?? AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
            else { return }
            let coordinator = AVCaptureDevice.RotationCoordinator(device: device, previewLayer: previewLayer)
            rotation = coordinator
            onRotation?(coordinator)
            applyRotation(coordinator.videoRotationAngleForHorizonLevelPreview)
            observation = coordinator.observe(\.videoRotationAngleForHorizonLevelPreview, options: [.new]) { [weak self] _, change in
                guard let angle = change.newValue else { return }
                Task { @MainActor in self?.applyRotation(angle) }
            }
        }

        private func applyRotation(_ angle: CGFloat) {
            guard let connection = previewLayer.connection, connection.isVideoRotationAngleSupported(angle) else { return }
            connection.videoRotationAngle = angle
        }
    }
}
