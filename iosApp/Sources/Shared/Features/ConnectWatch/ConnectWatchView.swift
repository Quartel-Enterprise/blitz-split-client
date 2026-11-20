import Observation
import Shared
import SwiftUI

@Observable
final class ConnectWatchModel {
    private(set) var state: ConnectWatchUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createConnectWatchViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ConnectWatchUiState
        // The latest state rather than the one emitted: an emission arrives a moment late, after `changeCode` already
        // read a newer code, and would take the field back a key
        watches.add(viewModel.stateWatcher.observe { [weak self] _ in
            self?.state = viewModel.uiState.value as! ConnectWatchUiState
        })
    }

    func send(_ event: any ConnectWatchUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Changes the code and reads the state back at once, as Restore's words do: the state otherwise arrives a moment
    /// later, and a key typed meanwhile would be overwritten; and when the ViewModel keeps the code as it was (a space,
    /// a seventh character), no new state arrives, and the field would keep showing what was typed.
    func changeCode(to text: String) {
        send(ConnectWatchUiEventOnCodeChanged(text: text))
        state = holder.viewModel.uiState.value as! ConnectWatchUiState
    }

    isolated deinit {
        holder.clear()
    }
}

/// Profile › "Connect watch" (15.14): the code an Apple Watch or a Wear OS watch shows when its phone is out of reach,
/// typed on any device with the account. A large sheet on iPhone, a form sheet on iPad and a sheet on Settings' window
/// on the Mac. A wrong or expired code (15.14b) and no internet (15.14c) turn Connect off and say why; once connected
/// only Close is left.
struct ConnectWatchView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ConnectWatchModel?

    var body: some View {
        Group {
            if let model {
                ConnectWatchContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(iOS)
        .navigationTitle("Connect watch")
        .navigationBarTitleDisplayMode(.inline)
        // A short form, so an iPad shows it compact as Android's and the web's dialog
        .formSheetSizing()
        #endif
        .onAppear {
            if model == nil { model = ConnectWatchModel() }
        }
    }
}

private struct ConnectWatchContent: View {
    let model: ConnectWatchModel

    private var state: ConnectWatchUiState {
        model.state
    }

    var body: some View {
        #if os(macOS)
        macBody
        #else
        iOSBody
        #endif
    }

    #if os(iOS)
    private var iOSBody: some View {
        Form {
            Section {
                VStack(spacing: 12) {
                    WatchTile(isConnected: state.isConnected, size: 64)
                    if state.isConnected {
                        Text("Watch connected")
                            .font(.title3.weight(.semibold))
                            .accessibilityAddTraits(.isHeader)
                    }
                    intro
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
            if !state.isConnected {
                Section {
                    CodeField(model: model)
                        .padding(.vertical, 2)
                } header: {
                    Text("Watch code")
                } footer: {
                    VStack(alignment: .leading, spacing: 8) {
                        notices
                        rules
                    }
                    .padding(.top, 4)
                }
            }
        }
        .formStyle(.grouped)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model.send(ConnectWatchUiEventOnCloseClicked.shared)
                }
            }
            if !state.isConnected {
                ToolbarItem(placement: .confirmationAction) {
                    if state.isConnecting {
                        ProgressView()
                    } else {
                        Button("Connect", systemImage: "checkmark", role: .confirm) {
                            model.send(ConnectWatchUiEventOnConnectClicked.shared)
                        }
                        .disabled(!state.canConnect)
                    }
                }
            }
        }
    }
    #endif

    #if os(macOS)
    /// A compact sheet on Settings' window: the tile beside what the sheet does, the code, and the buttons in a row.
    private var macBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 14) {
                WatchTile(isConnected: state.isConnected, size: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Group {
                        if state.isConnected {
                            Text("Watch connected")
                        } else {
                            Text("Connect watch")
                        }
                    }
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                    intro
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            if !state.isConnected {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Watch code")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    CodeField(model: model)
                        .padding(10)
                        .background(.quaternary.opacity(0.5), in: .rect(cornerRadius: 10))
                }
                VStack(alignment: .leading, spacing: 6) {
                    notices
                    rules
                }
                .font(.footnote)
            }
            HStack(spacing: 8) {
                Spacer(minLength: 8)
                if state.isConnected {
                    Button("Close") {
                        model.send(ConnectWatchUiEventOnCloseClicked.shared)
                    }
                    .keyboardShortcut(.defaultAction)
                } else {
                    Button("Close", role: .cancel) {
                        model.send(ConnectWatchUiEventOnCloseClicked.shared)
                    }
                    .keyboardShortcut(.cancelAction)
                    if state.isConnecting {
                        ProgressView()
                            .controlSize(.small)
                    }
                    Button("Connect") {
                        model.send(ConnectWatchUiEventOnConnectClicked.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .prominentLabel()
                    .keyboardShortcut(.defaultAction)
                    .disabled(!state.canConnect)
                }
            }
        }
        .padding(20)
        .frame(width: 460)
        .fixedSize(horizontal: false, vertical: true)
    }
    #endif

    @ViewBuilder
    private var intro: some View {
        if state.isConnected {
            Text("It sees your groups and syncs on its own. To disconnect it, sign out on the watch.")
        } else {
            Text("Type the code your watch shows. It signs in to your account and then syncs on its own.")
        }
    }

    /// Why Connect is off: a code the server refused, or no internet. VoiceOver reads it as it shows up.
    @ViewBuilder
    private var notices: some View {
        if let error = ConnectWatchCopy.error(state.error) {
            Label {
                Text(error)
            } icon: {
                Image(systemName: "exclamationmark.circle")
            }
            .foregroundStyle(Palette.negative)
            .accessibilityAddTraits(.updatesFrequently)
        }
        if state.showsOfflineNotice {
            Label {
                Text("Connecting the watch needs the internet.")
            } icon: {
                Image(systemName: "wifi.slash")
            }
            // The label's own secondary, never one relative to the section footer's, which would fade it further
            .foregroundStyle(Color.secondary)
        }
    }

    /// The server hands the account to whoever holds the watch's secret, so only a code of one's own watch is typed.
    private var rules: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Type only a code shown on your own watch: whoever has that watch gets into your account.")
            Text("The code is valid for 5 minutes, once.")
        }
        .foregroundStyle(Color.secondary)
    }
}

/// One field drawn as six boxes in two groups of three ("K7P 4QX"), so the keyboard, a paste or the one-time code
/// suggestion fills them all. The field itself is invisible over the boxes and is what VoiceOver reads.
private struct CodeField: View {
    let model: ConnectWatchModel

    @FocusState private var isFocused: Bool

    private var state: ConnectWatchUiState {
        model.state
    }

    var body: some View {
        ZStack {
            TextField(
                "Watch code",
                text: Binding(
                    get: { state.code },
                    set: { model.changeCode(to: $0) },
                ),
            )
                .focused($isFocused)
                .autocorrectionDisabled()
                #if os(iOS)
                .textInputAutocapitalization(.characters)
                .keyboardType(.asciiCapable)
                .textContentType(.oneTimeCode)
                #endif
                .submitLabel(.go)
                .onSubmit { model.send(ConnectWatchUiEventOnConnectClicked.shared) }
                .textFieldStyle(.plain)
                .foregroundStyle(.clear)
                .tint(.clear)
                .opacity(0.02)
                .accessibilityLabel("Watch code")
            boxes
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        }
        .contentShape(.rect)
        .onTapGesture { isFocused = true }
        .disabled(state.isConnecting)
        // The field is disabled while connecting, which drops its focus, so a refused code is typed again at once
        .onChange(of: state.isConnecting, initial: true) { _, isConnecting in
            if !isConnecting { isFocused = true }
        }
    }

    private var boxes: some View {
        // The code's characters once per draw, not once per box
        let characters = Array(state.code)
        return HStack(spacing: 6) {
            ForEach(0 ..< Int(state.codeLength), id: \.self) { index in
                if index == 3 { Spacer().frame(width: 4) }
                CodeBox(
                    character: index < characters.count ? String(characters[index]) : nil,
                    hasError: state.isCodeWrong,
                    isCurrent: isFocused && index == characters.count,
                )
            }
        }
        .frame(maxWidth: .infinity)
        // A Latin code reads left to right in Arabic and Hebrew too, as the watch shows it
        .environment(\.layoutDirection, .leftToRight)
    }
}

private struct CodeBox: View {
    let character: String?
    let hasError: Bool
    let isCurrent: Bool

    var body: some View {
        Text(verbatim: character ?? "")
            .font(.system(.title2, design: .monospaced, weight: .semibold))
            .minimumScaleFactor(0.6)
            .lineLimit(1)
            .frame(minWidth: 0, maxWidth: 44, minHeight: 54)
            .background(Color.secondary.opacity(0.14), in: .rect(cornerRadius: 12))
            .overlay {
                if hasError || isCurrent {
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(hasError ? Palette.negative : Palette.accent, lineWidth: 1.5)
                }
            }
    }
}

/// The watch on the accent tint that heads the sheet, a check once it connected.
private struct WatchTile: View {
    let isConnected: Bool
    let size: CGFloat

    var body: some View {
        Image(systemName: isConnected ? "checkmark" : "applewatch")
            .font(.system(size: size * 0.48, weight: .semibold))
            .foregroundStyle(Palette.accent)
            .frame(width: size, height: size)
            .background(Palette.accent.opacity(0.14), in: .rect(cornerRadius: size / 4))
            .accessibilityHidden(true)
    }
}

private enum ConnectWatchCopy {
    /// Why Connect didn't connect the watch, in the app's language; nil without an error.
    static func error(_ error: ConnectWatchError?) -> LocalizedStringKey? {
        guard let error else { return nil }
        switch error {
        case .invalidOrExpired: return "Wrong or expired code. Get a new one on the watch."
        case .tooManyAttempts: return "Too many wrong codes. Wait a few minutes and try again."
        case .needsAccount: return "Connect an account first: your data is only on this device."
        default: return "Couldn't connect the watch. Try again."
        }
    }
}
