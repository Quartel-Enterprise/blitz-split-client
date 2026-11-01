#if os(macOS)
import SwiftUI

/// The sections Ajustes' sidebar lists, in the order the profile's form draws them (F4Mac).
enum ProfileSettingsPane: CaseIterable, Hashable, Identifiable {
    case profile
    case keys
    case account
    case appearance
    case notifications
    case privacy

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .profile: "Profile"
        case .keys: "Keys to receive payments"
        case .account: "Account and sync"
        case .appearance: "Appearance"
        case .notifications: "Notifications"
        case .privacy: "Privacy and about"
        }
    }

    var symbol: String {
        switch self {
        case .profile: "person.crop.circle"
        case .keys: "key"
        case .account: "arrow.triangle.2.circlepath"
        case .appearance: "circle.lefthalf.filled"
        case .notifications: "bell"
        case .privacy: "hand.raised"
        }
    }
}

/// Ajustes (⌘,): the Mac's profile lives in its Settings window, as the notes ask, never pushed over the groups. The
/// sidebar lists the profile's sections and scrolls the form to the one picked; a narrow window (under 700 pt, as
/// Saldos' inspector) hides it behind the toolbar's sidebar button, one column. A key and deleting the account open
/// as sheets on this window (`AppModel.settingsSheet`). Until a profile exists, and once signing out erases it, there
/// is nothing to set, so the window closes.
struct SettingsView: View {
    @Bindable var app: AppModel

    @Environment(\.dismiss) private var dismiss
    @State private var pane: ProfileSettingsPane? = .profile
    @State private var columnVisibility = NavigationSplitViewVisibility.all

    var body: some View {
        Group {
            if app.hasProfile {
                NavigationSplitView(columnVisibility: $columnVisibility) {
                    List(ProfileSettingsPane.allCases, selection: $pane) { pane in
                        Label(pane.title, systemImage: pane.symbol)
                            .tag(pane)
                    }
                    .navigationSplitViewColumnWidth(min: 180, ideal: 220, max: 280)
                } detail: {
                    ProfileView(pane: pane)
                }
                .onGeometryChange(for: Bool.self, of: { $0.size.width < 700 }) { isNarrow in
                    columnVisibility = isNarrow ? .detailOnly : .all
                }
            } else {
                Palette.groupedBackground
            }
        }
        .frame(minWidth: 520, idealWidth: 880, minHeight: 480, idealHeight: 640)
        .sheet(item: $app.settingsSheet) { route in
            NavigationStack {
                switch route {
                case let .paymentKey(route): PaymentKeyView(route: route)
                case .deleteAccount: DeleteAccountView()
                default: EmptyView()
                }
            }
        }
        .tint(Palette.accent)
        .onChange(of: app.hasProfile, initial: true) { _, hasProfile in
            if !hasProfile { dismiss() }
        }
    }
}
#endif
