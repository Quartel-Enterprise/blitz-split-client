import Observation
import Shared
import SwiftUI

@Observable
final class DeleteAccountModel {
    private(set) var state: DeleteAccountUiState
    /// The JSON of "Export my data first", until the save sheet closes.
    var exportJson: String?

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createDeleteAccountViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! DeleteAccountUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if let export = action as? DeleteAccountUiActionSaveExport {
                self?.exportJson = export.json
            }
        })
    }

    func send(_ event: any DeleteAccountUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Delete account and data": a sheet on iPhone, iPad and Mac. It lists what goes, offers the export
/// first, and unlocks the button once the person says it can't be undone. Offline, the switch and the
/// button dim with the reason above them.
struct DeleteAccountView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws, and
    /// a ViewModel made for nothing would still start watching the account.
    @State private var model: DeleteAccountModel?

    var body: some View {
        Group {
            if let model {
                DeleteAccountContent(model: model)
                    .dataExporter(json: Binding(
                        get: { model.exportJson },
                        set: { model.exportJson = $0 },
                    ))
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Delete account")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model?.send(DeleteAccountUiEventOnBackClicked.shared)
                }
                .disabled(model?.state.isDeleting == true)
            }
        }
        #endif
        .interactiveDismissDisabled(model?.state.isDeleting == true)
        .onAppear {
            if model == nil { model = DeleteAccountModel() }
        }
    }
}

private struct DeleteAccountContent: View {
    let model: DeleteAccountModel

    private var state: DeleteAccountUiState {
        model.state
    }

    /// The canvas lists the provider's link only while there is one to name.
    private var deletedItems: [String] {
        var items = [
            String(localized: "Your profile (name, photo and color)"),
            String(localized: "Your Pix and Bitcoin keys"),
        ]
        if let provider = state.accountProvider?.displayName {
            items.append(String(localized: "The link to your \(provider) account"))
        }
        items.append(String(localized: "The copy of your data on this \(DeviceName.current)"))
        return items
    }

    private var confirmBinding: Binding<Bool> {
        Binding(
            get: { model.state.hasConfirmed },
            set: { model.send(DeleteAccountUiEventOnConfirmChanged(isChecked: $0)) },
        )
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
        ScrollView {
            VStack(spacing: 12) {
                if state.showsOfflineNotice {
                    offlineNotice
                }
                VStack(spacing: 8) {
                    WarningTile(size: 48, shape: .circle)
                    Text("Delete your account and your data?")
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                }
                .padding(.top, 10)
                VStack(alignment: .leading, spacing: 6) {
                    Text("What gets deleted")
                        .font(.footnote)
                        .textCase(.uppercase)
                        .foregroundStyle(.secondary)
                    ForEach(deletedItems, id: \.self) { item in
                        HStack(spacing: 8) {
                            Circle()
                                .fill(Palette.negative)
                                .frame(width: 5, height: 5)
                                .accessibilityHidden(true)
                            Text(verbatim: item)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 22))
                exportButton
            }
            .padding(.horizontal, 16)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .background(Palette.groupedBackground)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                Toggle("I understand this can't be undone", isOn: confirmBinding)
                    .disabled(!state.isConfirmEnabled)
                    .padding(.horizontal, 16)
                    .frame(minHeight: 48)
                    .background(Palette.card, in: .rect(cornerRadius: 22))
                if state.hasFailed {
                    errorLine
                }
                Button(role: .destructive) {
                    model.send(DeleteAccountUiEventOnDeleteClicked.shared)
                } label: {
                    Group {
                        if state.isDeleting {
                            ProgressView()
                        } else {
                            Text("Delete account")
                        }
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.capsule)
                .controlSize(.large)
                .tint(Palette.negative)
                .disabled(!state.canDelete)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
            .background(Palette.groupedBackground)
        }
    }
    #endif

    #if os(macOS)
    /// A compact sheet on the window, as the canvas draws it: the warning and the title side by side,
    /// what goes in one box, and the buttons in a row with the export on the left.
    private var macBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 14) {
                WarningTile(size: 44, shape: .rect(cornerRadius: 12))
                Text("Delete your account and your data?")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
            }
            if state.showsOfflineNotice {
                offlineNotice
            }
            VStack(alignment: .leading, spacing: 4) {
                Text("What gets deleted")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(verbatim: deletedItems.joined(separator: " · "))
                    .font(.callout)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.card, in: .rect(cornerRadius: 10))
            Toggle("I understand this can't be undone", isOn: confirmBinding)
                .toggleStyle(.checkbox)
                .disabled(!state.isConfirmEnabled)
            if state.hasFailed {
                errorLine
            }
            HStack(spacing: 8) {
                exportButton
                Spacer(minLength: 8)
                Button("Cancel", role: .cancel) {
                    model.send(DeleteAccountUiEventOnBackClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
                .disabled(state.isDeleting)
                Button(role: .destructive) {
                    model.send(DeleteAccountUiEventOnDeleteClicked.shared)
                } label: {
                    if state.isDeleting {
                        ProgressView().controlSize(.small)
                    } else {
                        Text("Delete account")
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Palette.negative)
                .disabled(!state.canDelete)
            }
        }
        .padding(20)
        .frame(width: 440)
        .fixedSize(horizontal: false, vertical: true)
    }
    #endif

    private var offlineNotice: some View {
        GlassNotice(
            title: "No internet.",
            message: "Deleting your account needs an internet connection. Connect and try again.",
        )
    }

    private var errorLine: some View {
        Text("Couldn't delete your account. Try again.")
            .font(.footnote)
            .foregroundStyle(Palette.negative)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var exportButton: some View {
        Button("Export my data first") {
            model.send(DeleteAccountUiEventOnExportClicked.shared)
        }
        .buttonStyle(.borderless)
        .foregroundStyle(Palette.accent)
    }
}

/// The warning triangle on the negative tint that heads the sheet.
private struct WarningTile<TileShape: Shape>: View {
    let size: CGFloat
    let shape: TileShape

    var body: some View {
        Image(systemName: "exclamationmark.triangle")
            .font(.system(size: size * 0.46, weight: .semibold))
            .foregroundStyle(Palette.negative)
            .frame(width: size, height: size)
            .background(Palette.negative.opacity(0.14), in: shape)
            .accessibilityHidden(true)
    }
}
