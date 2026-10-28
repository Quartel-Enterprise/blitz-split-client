import Observation
import Shared
import SwiftUI

@Observable
final class GroupsModel {
    private(set) var state: GroupsUiState
    private(set) var comingSoonCount = 0

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createGroupsViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is GroupsUiActionShowComingSoon { self?.comingSoonCount += 1 }
        })
    }

    func send(_ event: any GroupsUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// The tabs of the canvas. Only Groups is built; the others answer "Coming soon".
enum MainTab: Hashable {
    case groups
    case balances
    case contacts
    case activity
}

/// iPhone: the floating Liquid Glass tab bar. iPad and Mac: the glass sidebar (sidebarAdaptable).
struct MainTabView: View {
    @State private var model = GroupsModel()
    @State private var showsComingSoon = false
    @State private var showsRecoveryCode = false

    var body: some View {
        TabView(selection: tabSelection) {
            Tab("Groups", systemImage: "person.3", value: MainTab.groups) {
                NavigationStack {
                    GroupsView(model: model)
                }
            }
            Tab("Balances", systemImage: "creditcard", value: MainTab.balances) { Color.clear }
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) { Color.clear }
            Tab("Activity", systemImage: "list.bullet.rectangle", value: MainTab.activity) { Color.clear }
        }
        .tabViewStyle(.sidebarAdaptable)
        .overlay(alignment: .top) {
            if showsComingSoon {
                Text("Coming soon")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .glassEffect(.regular, in: .capsule)
                    .padding(.top, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .onChange(of: model.comingSoonCount) {
            withAnimation { showsComingSoon = true }
            Task {
                try? await Task.sleep(for: .seconds(2))
                withAnimation { showsComingSoon = false }
            }
        }
        // Shown once the first sync issued a code; the sheet settles the prompt itself ("I've saved it",
        // "Remind me later" or a swipe down), which hides it here too.
        .onChange(of: model.state.showsRecoveryCodePrompt, initial: true) { _, isDue in
            showsRecoveryCode = isDue
        }
        .sheet(isPresented: $showsRecoveryCode) {
            RecoveryCodeSheet()
        }
    }

    // Selecting a tab that is not built yet keeps Groups and says "Coming soon".
    private var tabSelection: Binding<MainTab> {
        Binding(
            get: { .groups },
            set: { tab in
                switch tab {
                case .balances: model.send(GroupsUiEventOnBalancesTabClicked.shared)
                case .contacts: model.send(GroupsUiEventOnContactsTabClicked.shared)
                case .activity: model.send(GroupsUiEventOnActivityTabClicked.shared)
                case .groups: break
                }
            },
        )
    }
}

struct GroupsView: View {
    let model: GroupsModel

    var body: some View {
        let state = model.state
        ContentUnavailableView {
            Label("No groups yet", systemImage: "person.3")
        } description: {
            Text("Create a group for the trip, the house or the barbecue. You can start even without internet.")
        } actions: {
            VStack(spacing: 10) {
                PrimaryButton(title: "Create group") {
                    model.send(GroupsUiEventOnCreateGroupClicked.shared)
                }
                TintedButton(title: "Join with a link or QR code") {
                    model.send(GroupsUiEventOnJoinWithLinkClicked.shared)
                }
            }
            .frame(maxWidth: 320)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Groups")
        .navigationSubtitle(syncText(state.syncBadge))
        .toolbar {
            if state.canRetrySync {
                ToolbarItem(placement: .primaryAction) {
                    Button("Try again", systemImage: "arrow.clockwise") {
                        model.send(GroupsUiEventOnRetrySyncClicked.shared)
                    }
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    model.send(GroupsUiEventOnProfileClicked.shared)
                } label: {
                    AvatarPhoto(
                        initial: state.avatarInitial,
                        color: state.avatarColor,
                        photoUrl: state.photoUrl,
                        size: 32,
                        font: .subheadline.weight(.semibold),
                    )
                }
                .accessibilityLabel("Profile")
            }
        }
    }

    // Everything synced shows no subtitle: the state only appears when it asks for attention.
    private func syncText(_ badge: SyncBadge?) -> Text {
        switch badge {
        case nil: Text(verbatim: "")
        case .offline: Text("Offline · everything is saved on your \(DeviceName.current)")
        case .failed: Text("No response from the server · try again")
        default: Text("Syncing…")
        }
    }
}
