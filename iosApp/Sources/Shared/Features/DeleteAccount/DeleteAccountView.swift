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
            String(localized: "Your profile (name, photo and color)", bundle: .appLanguage),
            String(localized: "Your Pix and Bitcoin keys", bundle: .appLanguage),
        ]
        if let provider = state.accountProvider.loaded(as: AccountProvider.self)?.displayName {
            items.append(String(localized: "The link to your \(provider) account", bundle: .appLanguage))
        }
        if state.deletesRecoveryCode {
            items.append(String(localized: "Your recovery code", bundle: .appLanguage))
        }
        items.append(String(localized: "The copy of your data on this \(DeviceName.current)", bundle: .appLanguage))
        return items
    }

    /// What the server keeps for the people the account shared groups with (testers' feedback): the
    /// groups with their bills and payments, the member it was, now with no account, and its comments
    /// and photos under its name.
    private var keptItems: [String] {
        [
            String(localized: "The groups, with their bills and payments, for the other members", bundle: .appLanguage),
            String(localized: "Your name in each group, as a member without an account", bundle: .appLanguage),
            String(localized: "Your comments and the photos you added, under your name", bundle: .appLanguage),
        ]
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
                ItemsCard(
                    title: "What gets deleted",
                    items: deletedItems,
                    bullet: Palette.negative,
                    isLoading: state.accountProvider.isLoading,
                )
                ItemsCard(title: "What stays in your groups", items: keptItems, bullet: .secondary)
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
                .prominentLabel(Palette.onNegative)
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
            MacItemsBox(
                title: "What gets deleted",
                items: deletedItems,
                isLoading: state.accountProvider.isLoading,
            )
            MacItemsBox(title: "What stays in your groups", items: keptItems)
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
                .prominentLabel(Palette.onNegative)
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

#if os(iOS)
/// One of the iOS lists ("What gets deleted", "What stays in your groups"): a title and a bullet per item. A
/// line still being read shimmers under them.
private struct ItemsCard: View {
    let title: LocalizedStringKey
    let items: [String]
    let bullet: Color
    var isLoading = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.footnote)
                .textCase(.uppercase)
                .foregroundStyle(.secondary)
            ForEach(items, id: \.self) { item in
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Circle()
                        .fill(bullet)
                        .frame(width: 5, height: 5)
                        .accessibilityHidden(true)
                    Text(verbatim: item)
                }
            }
            if isLoading {
                // The account not read yet: its link's line shimmers rather than being left out
                ShimmerLine(width: 220, height: 12)
                    .shimmering()
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 22))
    }
}
#endif

#if os(macOS)
/// One of the Mac's compact lists: a title and the items in one line, as the canvas draws them.
private struct MacItemsBox: View {
    let title: LocalizedStringKey
    let items: [String]
    var isLoading = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            Text(verbatim: items.joined(separator: " · "))
                .font(.callout)
                .fixedSize(horizontal: false, vertical: true)
            if isLoading {
                ShimmerLine(width: 220, height: 12)
                    .shimmering()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 10))
    }
}
#endif
