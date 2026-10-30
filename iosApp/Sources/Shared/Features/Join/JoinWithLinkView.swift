@preconcurrency import AVFoundation
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
        watches.add(viewModel.actionWatcher.observe { action in
            if action is JoinWithLinkUiActionOpenCameraSettings { CameraSettings.open() }
        })
    }

    /// Read straight from the ViewModel, so the field never shows a value a keystroke behind.
    var current: JoinWithLinkUiState {
        (holder.viewModel.uiState.value as? JoinWithLinkUiState) ?? state
    }

    func send(_ event: any JoinWithLinkUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The camera is only asked for once the reader opens, and read again on coming back from Settings.
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
            .task { await model.checkCameraAccess() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { Task { await model.checkCameraAccess() } }
            }
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
                    .background(Palette.groupedBackground, in: UnevenRoundedRectangle(
                        topLeadingRadius: 38,
                        bottomLeadingRadius: 38,
                    ))
                    .ignoresSafeArea(edges: .vertical)
            }
            .background(Color.black.ignoresSafeArea())
        } else {
            VStack(spacing: 0) {
                cameraArea
                pastePanel
                    .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                    .frame(maxWidth: .infinity)
                    .background(Palette.groupedBackground, in: UnevenRoundedRectangle(
                        topLeadingRadius: 38,
                        topTrailingRadius: 38,
                    ))
                    .ignoresSafeArea(edges: .bottom)
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

    private var cameraArea: some View {
        // Why: the camera reaches under the status bar, and its inset puts the close and flashlight buttons below it.
        GeometryReader { proxy in
            cameraLayers(model.state, topInset: proxy.safeAreaInsets.top)
        }
        .environment(\.colorScheme, .dark)
        .clipped()
        .ignoresSafeArea(edges: .top)
    }

    private func cameraLayers(_ state: JoinWithLinkUiState, topInset: CGFloat) -> some View {
        ZStack {
            Color.black
            if state.cameraAccess == .granted {
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
            VStack(spacing: 0) {
                topBar(state, topInset: topInset)
                Spacer(minLength: 8)
                if state.cameraAccess == .denied {
                    cameraDenied
                } else if state.isCameraUnavailable {
                    cameraUnavailable
                } else {
                    viewfinder(state)
                }
                Spacer(minLength: 8)
            }
            .padding(.horizontal, 16)
        }
    }

    private func topBar(_ state: JoinWithLinkUiState, topInset: CGFloat) -> some View {
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
        .padding(.top, topInset + 8)
        #else
        .padding(.top, topInset + 16)
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

    private var cameraDenied: some View {
        VStack(spacing: 10) {
            Image(systemName: "video.slash")
                .font(.system(size: 52))
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
        .foregroundStyle(.white)
        .multilineTextAlignment(.center)
        .frame(maxWidth: 420)
        .padding(.horizontal, 20)
    }

    private var cameraUnavailable: some View {
        VStack(spacing: 10) {
            Image(systemName: "video.slash")
                .font(.system(size: 52))
            Text("Couldn't open the camera. Paste the link below.")
                .padding(.top, 8)
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
