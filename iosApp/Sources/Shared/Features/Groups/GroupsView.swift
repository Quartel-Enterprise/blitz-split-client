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

/// The tabs of the canvas. Only Grupos is built; the others answer "Em breve".
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
            Tab("Grupos", systemImage: "person.3", value: MainTab.groups) {
                NavigationStack {
                    GroupsView(model: model)
                }
            }
            Tab("Saldos", systemImage: "creditcard", value: MainTab.balances) { Color.clear }
            Tab("Contatos", systemImage: "person.crop.circle", value: MainTab.contacts) { Color.clear }
            Tab("Atividade", systemImage: "list.bullet.rectangle", value: MainTab.activity) { Color.clear }
        }
        .tabViewStyle(.sidebarAdaptable)
        .overlay(alignment: .top) {
            if showsComingSoon {
                Text("Em breve")
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
        // Shown once the first sync issued a code; the sheet settles the prompt itself ("Já guardei",
        // "Lembrar depois" or a swipe down), which hides it here too.
        .onChange(of: model.state.showsRecoveryCodePrompt, initial: true) { _, isDue in
            showsRecoveryCode = isDue
        }
        .sheet(isPresented: $showsRecoveryCode) {
            RecoveryCodeSheet()
        }
    }

    // Selecting a tab that is not built yet keeps Grupos and says "Em breve".
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
            Label("Nenhum grupo ainda", systemImage: "person.3")
        } description: {
            Text("Crie um grupo para a viagem, a casa ou o churrasco. Dá para começar mesmo sem internet.")
        } actions: {
            VStack(spacing: 10) {
                PrimaryButton(title: "Criar grupo") {
                    model.send(GroupsUiEventOnCreateGroupClicked.shared)
                }
                TintedButton(title: "Entrar com link ou QR") {
                    model.send(GroupsUiEventOnJoinWithLinkClicked.shared)
                }
            }
            .frame(maxWidth: 320)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Grupos")
        .navigationSubtitle(syncText(state.syncBadge))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    model.send(GroupsUiEventOnProfileClicked.shared)
                } label: {
                    Text(state.avatarInitial)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(state.avatarColor.content)
                        .frame(width: 32, height: 32)
                        .background(state.avatarColor.container, in: .circle)
                }
                .accessibilityLabel("Perfil")
            }
        }
    }

    private func syncText(_ badge: SyncBadge) -> String {
        switch badge {
        case .offline: "Offline · tudo fica salvo no \(DeviceName.current)"
        case .synced: "Sincronizado agora"
        default: "Sincronizando…"
        }
    }
}
