@preconcurrency import AVFoundation
import Combine
import Observation
import Shared
import SwiftUI

#if os(iOS)
import UIKit
#else
import AppKit
#endif

@Observable
final class JoinWithLinkModel {
    private(set) var state: JoinWithLinkUiState

    @ObservationIgnored private let holder: ViewModelHolder<JoinWithLinkViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteJoinWithLink) {
        holder = BlitzSplitApple.shared.createJoinWithLinkViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! JoinWithLinkUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is JoinWithLinkUiActionRequestCameraAccess {
                Task { await self?.checkCameraAccess() }
            } else if action is JoinWithLinkUiActionOpenCameraSettings {
                CameraSettings.open()
            }
        })
    }

    /// Read straight from the ViewModel, so the field never shows a value a keystroke behind.
    var current: JoinWithLinkUiState {
        (holder.viewModel.uiState.value as? JoinWithLinkUiState) ?? state
    }

    func send(_ event: any JoinWithLinkUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// A Mac mini or a Mac Studio may have no camera at all: then "Open camera" never shows, only the link.
    func checkHasCamera() {
        send(JoinWithLinkUiEventOnCameraChecked(hasCamera: AVCaptureDevice.default(for: .video) != nil))
    }

    /// Asked only after "Open camera", and read again on coming back from Settings once it was.
    func checkCameraAccess() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            send(JoinWithLinkUiEventOnCameraAccessChanged(isGranted: true))
        case .notDetermined:
            let isGranted = await AVCaptureDevice.requestAccess(for: .video)
            send(JoinWithLinkUiEventOnCameraAccessChanged(isGranted: isGranted))
        default:
            send(JoinWithLinkUiEventOnCameraAccessChanged(isGranted: false))
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Join with an invite" (3.0): a QR read by the camera or a link pasted. The camera stays dark in both themes, and
/// only the "Have the link?" panel follows the theme. It covers the whole screen on iPhone and iPad, and is a sheet on
/// the Mac.
struct JoinWithLinkView: View {
    let route: RouteJoinWithLink

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: JoinWithLinkModel?

    var body: some View {
        Group {
            if let model {
                JoinWithLinkContent(model: model)
            } else {
                Color.black.ignoresSafeArea()
            }
        }
        .onAppear {
            if model == nil { model = JoinWithLinkModel(route: route) }
        }
    }
}

private struct JoinWithLinkContent: View {
    let model: JoinWithLinkModel

    @Environment(\.scenePhase) private var scenePhase

    #if os(iOS)
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    var body: some View {
        layout
            .task { model.checkHasCamera() }
            // Why: a webcam plugged in or out, or an iPhone nearby as a Mac's Continuity Camera, while the reader is open.
            // AVFoundation may post these off the main thread, and the state they change is drawn on it.
            .onReceive(cameraChanges) { _ in model.checkHasCamera() }
            .onChange(of: scenePhase) { _, phase in
                // Why: before "Open camera" nothing is asked, not even on coming back to the app; while the system's alert is
                // still open, the "Open camera" request answers on its own.
                if phase == .active, model.state.isCameraRequested, model.state.cameraAccess != .asking {
                    Task { await model.checkCameraAccess() }
                }
            }
    }

    private var cameraChanges: some Publisher<Notification, Never> {
        let center = NotificationCenter.default
        return center.publisher(for: AVCaptureDevice.wasConnectedNotification)
            .merge(with: center.publisher(for: AVCaptureDevice.wasDisconnectedNotification))
            .receive(on: DispatchQueue.main)
    }

    @ViewBuilder
    private var layout: some View {
        #if os(iOS)
        if verticalSizeClass == .compact {
            // A phone lying down: the camera beside the panel, so the field never hides behind it.
            HStack(spacing: 0) {
                cameraArea
                pastePanel
                    .frame(width: 380)
                    .frame(maxHeight: .infinity)
                    .background {
                        // Why: the panel meets the screen's edge, past the trailing inset of a phone lying down.
                        panelBackground(
                            UnevenRoundedRectangle(topLeadingRadius: 38, bottomLeadingRadius: 38),
                            edges: [.vertical, .trailing],
                        )
                    }
                    .ignoresSafeArea(.container, edges: .vertical)
            }
            .background(Color.black.ignoresSafeArea())
        } else {
            VStack(spacing: 0) {
                cameraArea
                pastePanel
                    .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                    .frame(maxWidth: .infinity)
                    .background {
                        panelBackground(
                            UnevenRoundedRectangle(topLeadingRadius: 38, topTrailingRadius: 38),
                            edges: .bottom,
                        )
                    }
                    .ignoresSafeArea(.container, edges: .bottom)
            }
            .background(Color.black.ignoresSafeArea())
        }
        #else
        VStack(spacing: 0) {
            cameraArea
                .frame(height: 380)
            pastePanel
                .background(Palette.groupedBackground)
        }
        .frame(width: 480)
        #endif
    }

    #if os(iOS)
    /// The link panel's color, which reaches under the keyboard too, while the keyboard pushes the panel above it.
    private func panelBackground(_ shape: some Shape, edges: Edge.Set) -> some View {
        shape
            .fill(Palette.groupedBackground)
            .ignoresSafeArea(edges: edges)
    }
    #endif

    private var cameraArea: some View {
        // Why: only the camera reaches under the status bar; the close and flashlight buttons stay below it, where the
        // system hands their taps to the app.
        cameraOverlay(model.state)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background {
                cameraBackground(model.state)
                    .clipped()
                    .ignoresSafeArea(edges: .top)
            }
            .environment(\.colorScheme, .dark)
    }

    private func cameraBackground(_ state: JoinWithLinkUiState) -> some View {
        ZStack {
            Color.black
            if state.showsCameraPreview {
                InviteScannerView(
                    isTorchOn: state.isTorchOn,
                    onScanned: { model.send(JoinWithLinkUiEventOnQrScanned(content: $0)) },
                    onOpened: { model.send(JoinWithLinkUiEventOnCameraOpened(hasTorch: $0)) },
                    onUnavailable: { model.send(JoinWithLinkUiEventOnCameraUnavailable.shared) },
                )
            }
            // The design's scrim, dark toward the edges, so the white marks and text read over a bright room.
            RadialGradient(colors: [.clear, .black.opacity(0.7)], center: .center, startRadius: 0, endRadius: 360)
                .allowsHitTesting(false)
        }
    }

    private func cameraOverlay(_ state: JoinWithLinkUiState) -> some View {
        VStack(spacing: 8) {
            topBar(state)
            Group {
                switch state.cameraStage {
                case .prompt: cameraPrompt
                case .noCamera: cameraMessage(noCameraMessage)
                case .denied: cameraDenied
                case .failed: cameraMessage("Couldn't open the camera. Paste the link below.")
                default: viewfinder(state)
                }
            }
            // Why: with the keyboard up on a small iPhone the camera area gets short; only this part is cut, so the
            // close button never goes back under the status bar nor this text over the link panel.
            .frame(maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
            .clipped()
            .padding(.bottom, 8)
        }
        .padding(.horizontal, 16)
    }

    private func topBar(_ state: JoinWithLinkUiState) -> some View {
        HStack {
            Button("Close", systemImage: "xmark") {
                model.send(JoinWithLinkUiEventOnCloseClicked.shared)
            }
            .labelStyle(.iconOnly)
            .buttonStyle(.glass)
            .buttonBorderShape(.circle)
            .controlSize(.large)
            #if os(macOS)
            .keyboardShortcut(.cancelAction)
            #endif
            Spacer()
            Text("Join with an invite")
                .font(.headline)
                .foregroundStyle(.white)
            Spacer()
            if state.showsTorch {
                Button(
                    state.isTorchOn ? "Turn off the flashlight" : "Turn on the flashlight",
                    systemImage: state.isTorchOn ? "flashlight.on.fill" : "flashlight.off.fill",
                ) {
                    model.send(JoinWithLinkUiEventOnTorchClicked.shared)
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .controlSize(.large)
            } else {
                // Keeps the title centered without the flashlight.
                Color.clear.frame(width: 44, height: 44)
            }
        }
        #if os(iOS)
        .padding(.top, 8)
        #else
        .padding(.top, 16)
        #endif
    }

    private func viewfinder(_ state: JoinWithLinkUiState) -> some View {
        VStack(spacing: 24) {
            ViewfinderCorners()
                .stroke(
                    state.isQrInvalid ? Palette.cameraError : .white,
                    style: StrokeStyle(lineWidth: 5, lineCap: .round),
                )
                .frame(maxWidth: 250, maxHeight: 250)
                .aspectRatio(1, contentMode: .fit)
                .accessibilityLabel("Camera viewfinder")
            if state.isQrInvalid {
                VStack(spacing: 8) {
                    Label("This QR code isn't a Blitz Split invite", systemImage: "exclamationmark.circle")
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .glassEffect(.regular, in: .capsule)
                    Text("Try another QR code or paste the link below.")
                        .font(.subheadline)
                }
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .accessibilityElement(children: .combine)
            } else {
                Text("Point the camera at the invite's QR code")
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
            }
        }
    }

    /// In the camera's place until "Open camera", the only tap that asks for the camera and opens it.
    private var cameraPrompt: some View {
        cameraPlaceholder(systemImage: "qrcode.viewfinder") {
            Text("Scan the invite's QR code")
                .font(.title2.bold())
                .padding(.top, 8)
            Text("The camera only opens when you tap the button.")
            Button("Open camera", systemImage: "camera") {
                model.send(JoinWithLinkUiEventOnOpenCameraClicked.shared)
            }
            .font(.headline)
            .buttonStyle(.glassProminent)
            .prominentLabel()
            .buttonBorderShape(.capsule)
            .controlSize(.large)
            .tint(Palette.accent)
            .padding(.top, 10)
        }
    }

    private var noCameraMessage: LocalizedStringKey {
        #if os(iOS)
        "This device has no camera. Paste the link below."
        #else
        "This Mac has no camera. Paste the link below."
        #endif
    }

    private var cameraDenied: some View {
        cameraPlaceholder(systemImage: "video.slash") {
            Text("No access to the camera")
                .font(.title2.bold())
                .padding(.top, 8)
            #if os(iOS)
            Text("To read the QR code, allow Blitz Split to use the camera in Settings. Or paste the link below.")
            #else
            Text("To read the QR code, allow Blitz Split to use the camera in System Settings. Or paste the link below.")
            #endif
            Button {
                model.send(JoinWithLinkUiEventOnOpenSettingsClicked.shared)
            } label: {
                #if os(iOS)
                Text("Open Settings")
                #else
                Text("Open System Settings")
                #endif
            }
            .buttonStyle(.glass)
            .buttonBorderShape(.capsule)
            .controlSize(.large)
            .padding(.top, 10)
        }
    }

    /// The camera's place with only a line to say why there is no camera to show.
    private func cameraMessage(_ message: LocalizedStringKey) -> some View {
        cameraPlaceholder(systemImage: "video.slash") {
            Text(message)
                .padding(.top, 8)
        }
    }

    /// What the camera's place shows instead of the camera: a symbol over the words and what can be done.
    private func cameraPlaceholder(systemImage: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.system(size: 52))
            content()
        }
        .foregroundStyle(.white)
        .multilineTextAlignment(.center)
        .frame(maxWidth: 420)
        .padding(.horizontal, 20)
    }

    private var pastePanel: some View {
        let state = model.state
        return ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Have the link?")
                        .font(.title3.bold())
                    Text("Paste the link someone sent you in a message.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                linkField(state)
                if state.isLinkInvalid {
                    Text("This link isn't a Blitz Split invite")
                        .font(.footnote)
                        .foregroundStyle(Palette.negative)
                        .padding(.horizontal, 16)
                }
                if state.canOpen {
                    PrimaryButton(title: "Open invite") {
                        model.send(JoinWithLinkUiEventOnOpenClicked.shared)
                    }
                    #if os(macOS)
                    .keyboardShortcut(.defaultAction)
                    #endif
                    .padding(.top, 4)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 22)
            .padding(.bottom, 24)
        }
        .scrollBounceBehavior(.basedOnSize)
        #if os(iOS)
        .frame(maxHeight: 280)
        .fixedSize(horizontal: false, vertical: verticalSizeClass != .compact)
        #endif
    }

    private func linkField(_ state: JoinWithLinkUiState) -> some View {
        HStack(spacing: 8) {
            TextField("Invite link", text: linkBinding, prompt: Text(verbatim: "blitzsplit.com/g/…"))
                .font(.body.monospaced())
                .textFieldStyle(.plain)
                .autocorrectionDisabled()
                #if os(iOS)
                .textInputAutocapitalization(.never)
                .keyboardType(.URL)
                #endif
                .submitLabel(.go)
                .onSubmit { model.send(JoinWithLinkUiEventOnOpenClicked.shared) }
            if state.showsPaste {
                // The system's paste control, which reads the clipboard without asking.
                PasteButton(payloadType: String.self) { strings in
                    model.send(JoinWithLinkUiEventOnPasted(text: strings.first ?? ""))
                }
                .labelStyle(.titleOnly)
                .buttonBorderShape(.capsule)
                .tint(Palette.accent)
            } else {
                Button("Clear", systemImage: "xmark.circle.fill") {
                    model.send(JoinWithLinkUiEventOnClearClicked.shared)
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .frame(width: 36, height: 36)
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 6)
        .frame(minHeight: 50)
        .background(Palette.card, in: .capsule)
    }

    private var linkBinding: Binding<String> {
        Binding(
            get: { model.current.link },
            set: { model.send(JoinWithLinkUiEventOnLinkChanged(link: $0)) },
        )
    }
}

/// The four rounded corners of the viewfinder, not a whole frame.
private nonisolated struct ViewfinderCorners: Shape {
    func path(in rect: CGRect) -> Path {
        let arm: CGFloat = 50
        let radius: CGFloat = 26
        var corner = Path()
        corner.move(to: CGPoint(x: 0, y: arm))
        corner.addLine(to: CGPoint(x: 0, y: radius))
        corner.addArc(
            center: CGPoint(x: radius, y: radius),
            radius: radius,
            startAngle: .degrees(180),
            endAngle: .degrees(270),
            clockwise: false,
        )
        corner.addLine(to: CGPoint(x: arm, y: 0))
        var path = Path()
        for degrees in [0.0, 90, 180, 270] {
            let turn = CGAffineTransform(translationX: rect.midX, y: rect.midY)
                .rotated(by: degrees * .pi / 180)
                .translatedBy(x: -rect.width / 2, y: -rect.height / 2)
            path.addPath(corner, transform: turn)
        }
        return path
    }
}

/// The app's page in the device's settings, where the camera can be allowed.
enum CameraSettings {
    static func open() {
        #if os(iOS)
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
        #else
        if let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_Camera") {
            NSWorkspace.shared.open(url)
        }
        #endif
    }
}
