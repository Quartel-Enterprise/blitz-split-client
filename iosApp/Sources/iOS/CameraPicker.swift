import AVFoundation
import SwiftUI
import UIKit

/// The system camera for the profile photo and a bill's photos. It hands back a JPEG, which the shared code shrinks
/// like a picked photo.
struct CameraPicker: UIViewControllerRepresentable {
    // Why: a camera the device restricts (Screen Time, MDM) can't be allowed in Settings, so it isn't offered.
    static var isAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
            && AVCaptureDevice.authorizationStatus(for: .video) != .restricted
    }

    /// The front camera for a profile photo, the back one for a bill's.
    var device: UIImagePickerController.CameraDevice = .front
    let onPicked: (Data?) -> Void

    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        if UIImagePickerController.isCameraDeviceAvailable(device) { picker.cameraDevice = device }
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ picker: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let parent: CameraPicker

        init(parent: CameraPicker) {
            self.parent = parent
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any],
        ) {
            let image = info[.originalImage] as? UIImage
            parent.onPicked(image?.jpegData(compressionQuality: 0.95))
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

/// "Take photo": asks for the camera first, then opens it full screen, or explains a "no" with the way to Settings.
private struct CameraCapture: ViewModifier {
    @Binding var isRequested: Bool
    let device: UIImagePickerController.CameraDevice
    let onPicked: (Data?) -> Void

    @State private var showsCamera = false
    @State private var showsCameraDenied = false

    func body(content: Content) -> some View {
        content
            .onChange(of: isRequested) { _, requested in
                guard requested else { return }
                isRequested = false
                Task { await open() }
            }
            .fullScreenCover(isPresented: $showsCamera) {
                CameraPicker(device: device, onPicked: onPicked)
                    .ignoresSafeArea()
            }
            .alert("Camera access is off", isPresented: $showsCameraDenied) {
                Button("Open Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Allow camera access in Settings to take a photo.")
            }
    }

    /// Why: the system camera shows a black screen when access was refused, so ask first and explain a "no".
    private func open() async {
        let isAllowed = switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: true
        case .notDetermined: await AVCaptureDevice.requestAccess(for: .video)
        default: false
        }
        if isAllowed { showsCamera = true } else { showsCameraDenied = true }
    }
}

extension View {
    /// Opens the camera each time `isRequested` turns true, handing back the photo's JPEG, or nil when it can't be read.
    func cameraCapture(
        isRequested: Binding<Bool>,
        device: UIImagePickerController.CameraDevice = .front,
        onPicked: @escaping (Data?) -> Void,
    ) -> some View {
        modifier(CameraCapture(isRequested: isRequested, device: device, onPicked: onPicked))
    }
}
