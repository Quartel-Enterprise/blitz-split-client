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
    /// The shared back stack above the groups: a group's members, the profile, its editor and, on iPhone, a key.
    @Binding var path: [AppRoute]

    @State private var model = GroupsModel()
    @State private var showsComingSoon = false
    @State private var showsRecoveryCode = false

    var body: some View {
        TabView(selection: tabSelection) {
            Tab("Groups", systemImage: "person.3", value: MainTab.groups) {
                NavigationStack(path: $path) {
                    GroupsView(model: model)
                        .navigationDestination(for: AppRoute.self) { route in
                            switch route {
                            case .editProfile: EditProfileView()
                            case .notificationSettings: NotificationSettingsView()
                            case let .paymentKey(route): PaymentKeyView(route: route)
                            case let .members(route): MembersView(route: route)
                            default: ProfileView()
                            }
                        }
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
        Group {
            if !state.groups.isEmpty {
                GroupsList(model: model)
            } else if state.showsEmptyState {
                emptyState
            } else {
                // Nothing until the groups are read, so the empty state never flashes before the list
                Color.clear
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Groups")
        // Why: on iPhone and iPad the selected Groups tab already names the screen, so the bar shows only the sync
        // status in the title's place. The title still names the screen in the back stack, and the status keeps it
        // as its VoiceOver label.
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                syncText(state.syncBadge)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    // Why: the slot sits between the bar's buttons, too narrow for the offline text on one line.
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .accessibilityLabel(Text("Groups"))
                    .accessibilityValue(syncText(state.syncBadge))
                    .accessibilityAddTraits(.isHeader)
            }
            .sharedBackgroundVisibility(.hidden)
        }
        #else
        .navigationSubtitle(syncText(state.syncBadge))
        #endif
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

    private var emptyState: some View {
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

/// The minimal list of the home until the full one (balances, the row's menu) arrives with the group's own
/// screen. A row opens the group; no group has expenses yet.
private struct GroupsList: View {
    let model: GroupsModel

    var body: some View {
        List {
            Section {
                ForEach(model.state.groups, id: \.id) { group in
                    Button {
                        model.send(GroupsUiEventOnGroupClicked(groupId: group.id))
                    } label: {
                        GroupListRow(group: group)
                    }
                    .buttonStyle(.plain)
                }
            } header: {
                HStack {
                    Text("Open")
                    Spacer()
                    Button("New group", systemImage: "plus") {
                        model.send(GroupsUiEventOnCreateGroupClicked.shared)
                    }
                    .buttonStyle(.bordered)
                    .textCase(nil)
                }
            }
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #endif
    }
}

private struct GroupListRow: View {
    let group: GroupRow

    private var members: Text {
        group.memberCount == 1 ? Text("Just you") : Text("\(group.memberCount) members")
    }

    var body: some View {
        HStack(spacing: 14) {
            GroupAvatar(initials: group.initials, color: group.color, size: 44)
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: group.name)
                    .font(.body.weight(.semibold))
                    .lineLimit(1)
                HStack(spacing: 6) {
                    HStack(spacing: -6) {
                        ForEach(Array(group.faces.enumerated()), id: \.offset) { _, face in
                            AvatarPhoto(initial: face.initial, color: face.color, photoUrl: nil, size: 20)
                        }
                        if group.extraMemberCount > 0 {
                            Text(verbatim: "+\(group.extraMemberCount)")
                                .font(.caption2.weight(.semibold))
                                .padding(.leading, 10)
                        }
                    }
                    .accessibilityHidden(true)
                    if group.isPending {
                        Circle()
                            .fill(.orange)
                            .frame(width: 8, height: 8)
                            .accessibilityHidden(true)
                    }
                    (group.isPending ? Text("\(members) · queued") : members)
                        .lineLimit(1)
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Text("no expenses")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .contentShape(.rect)
    }
}
