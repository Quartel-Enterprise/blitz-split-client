import Observation
import Shared
import SwiftUI

@Observable
final class AgendaModel {
    private(set) var state: AgendaUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createAgendaViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! AgendaUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is AgendaUiActionRequestAccess: self?.requestAccess()
            case is AgendaUiActionOpenSettings: AgendaReader.openSettings()
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now: the search field reads its text back at once.
    var current: AgendaUiState {
        (holder.viewModel.uiState.value as? AgendaUiState) ?? state
    }

    func send(_ event: any AgendaUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    var query: Binding<String> {
        Binding(
            get: { self.current.query },
            set: { self.send(AgendaUiEventOnQueryChanged(query: $0)) },
        )
    }

    /// What the system says now, and the people once it lets the app read them: on opening and back from Settings.
    func refresh() {
        let access = AgendaReader.access
        send(AgendaUiEventOnAccessChanged(access: access))
        guard AgendaReader.canRead else { return }
        Task { [weak self] in
            let contacts = await AgendaReader.load()
            self?.send(AgendaUiEventOnContactsLoaded(contacts: contacts))
        }
    }

    /// The system's own question, then what it left.
    private func requestAccess() {
        Task { [weak self] in
            _ = await AgendaReader.requestAccess()
            self?.refresh()
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite from contacts" (11.3): the device's contacts, each invited with one of your groups' links; nobody is told
/// who already uses the app, since no number leaves the device. Before the system asks, a screen says why; refused, it
/// leads to Settings and offers the link. iOS's limited access lists only the contacts picked. Pushed over Contacts,
/// or in the column beside the list (`isInPane`).
struct AgendaView: View {
    var isInPane = false

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: AgendaModel?
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if let model, model.state.showsContacts {
                AgendaList(model: model)
                    .searchable(text: model.query, prompt: Text("Search contacts"))
            } else if let model, model.state.showsAskAccess {
                AgendaMessage(
                    title: "Bring in people from your contacts",
                    message: "Invite someone from your contacts with one of your groups' links. Their numbers stay on this \(DeviceName.current).",
                    systemImage: "person.crop.circle.badge.plus",
                    actionTitle: "Allow access to contacts",
                ) {
                    model.send(AgendaUiEventOnAllowAccessClicked.shared)
                } onInviteWithLink: {
                    model.send(AgendaUiEventOnInviteWithLinkClicked.shared)
                }
            } else if let model, model.state.showsDenied {
                AgendaMessage(
                    title: "No access to contacts",
                    message: deniedMessage,
                    systemImage: "person.crop.circle.badge.xmark",
                    actionTitle: settingsTitle,
                ) {
                    model.send(AgendaUiEventOnOpenSettingsClicked.shared)
                } onInviteWithLink: {
                    model.send(AgendaUiEventOnInviteWithLinkClicked.shared)
                }
            } else {
                // Until the system says what access there is
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Invite from contacts")
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        #endif
        .onAppear {
            if model == nil {
                let model = AgendaModel()
                self.model = model
                model.refresh()
            }
        }
        // Back from Settings, the access may have changed
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model?.refresh() }
        }
    }

    private var deniedMessage: LocalizedStringKey {
        #if os(macOS)
        "Access to contacts was denied. To invite from your contacts, allow Contacts in System Settings. The invite link works without it."
        #else
        "Access to contacts was denied. To invite from your contacts, allow Contacts in Settings. The invite link works without it."
        #endif
    }

    private var settingsTitle: LocalizedStringKey {
        #if os(macOS)
        "Open System Settings"
        #else
        "Open Settings"
        #endif
    }
}

/// Before the system asks, and once it was refused: why, the one way forward, and the link, which needs no access.
private struct AgendaMessage: View {
    let title: LocalizedStringKey
    let message: LocalizedStringKey
    let systemImage: String
    let actionTitle: LocalizedStringKey
    let action: () -> Void
    let onInviteWithLink: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: systemImage)
                    .font(.system(size: 44))
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
                Text(title)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                Text(message)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                VStack(spacing: 10) {
                    PrimaryButton(title: actionTitle, action: action)
                    TintedButton(title: "Invite with a link or QR code", action: onInviteWithLink)
                }
                .padding(.top, 8)
            }
            .padding(24)
            .padding(.top, 24)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
    }
}

/// "In your contacts · 148", A to Z, each with Invite; under limited access, a note that only the ones picked show.
private struct AgendaList: View {
    let model: AgendaModel

    var body: some View {
        let state = model.state
        List {
            if state.access == .limited {
                Section {
                    Label("Only the contacts you picked show here.", systemImage: "info.circle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            if state.showsNoResults {
                Section {
                    ContentUnavailableView.search(text: state.query)
                }
                .listRowBackground(Color.clear)
            } else {
                Section {
                    ForEach(state.results, id: \.contact.id) { match in
                        AgendaContactRow(match: match) {
                            model.send(AgendaUiEventOnInviteClicked(contactId: match.contact.id))
                        }
                    }
                } header: {
                    Text("In your contacts · \(state.results.count)")
                }
            }
            Section {
                Button("Invite with a link or QR code", systemImage: "qrcode") {
                    model.send(AgendaUiEventOnInviteWithLinkClicked.shared)
                }
            }
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #endif
        .scrollContentBackground(.hidden)
    }
}
