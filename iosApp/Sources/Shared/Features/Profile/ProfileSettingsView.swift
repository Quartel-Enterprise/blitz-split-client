#if os(macOS)
import SwiftUI

/// The sections Ajustes' sidebar lists, in the order the profile's form draws them (F4Mac), then Categories, the
/// category map (9.1), which takes the form's place.
enum ProfileSettingsPane: CaseIterable, Hashable, Identifiable {
    case profile
    case iphone
    case keys
    case account
    case appearance
    case notifications
    case privacy
    case categories

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .profile: "Profile"
        case .iphone: "iPhone"
        case .keys: "Keys to receive payments"
        case .account: "Account and sync"
        case .appearance: "Appearance"
        case .notifications: "Notifications"
        case .privacy: "Privacy and about"
        case .categories: "Categories"
        }
    }

    var symbol: String {
        switch self {
        case .profile: "person.crop.circle"
        case .iphone: "iphone"
        case .keys: "key"
        case .account: "arrow.triangle.2.circlepath"
        case .appearance: "circle.lefthalf.filled"
        case .notifications: "bell"
        case .privacy: "hand.raised"
        case .categories: "tag"
        }
    }
}

/// Ajustes (⌘,): the Mac's profile lives in its Settings window, as the notes ask, never pushed over the groups. The
/// sidebar lists the profile's sections and scrolls the form to the one picked, and Categories shows the category map
/// in the form's place (whatever opens the map opens it here, `AppModel.requestedSettingsPane`); a narrow window
/// (under 700 pt, as Saldos' inspector) hides it behind the toolbar's sidebar button, one column. A key and deleting the account open
/// as sheets on this window (`AppModel.settingsSheet`), and so does the photo big. Until a profile exists, and once signing out erases it, there
/// is nothing to set, so the window closes.
struct SettingsView: View {
    @Bindable var app: AppModel

    @Environment(\.dismiss) private var dismiss
    /// The profile's ViewModel, made once a profile exists and dropped once none is left, so redrawing this window (a
    /// sidebar click, the main window moving the back stack) never makes another.
    @State private var model: ProfileModel?
    @State private var pane: ProfileSettingsPane? = .profile
    /// Bumped on every pick in the sidebar, the section already picked included, so the form scrolls back to it.
    @State private var scrollRequest = 0
    @State private var columnVisibility = NavigationSplitViewVisibility.all

    var body: some View {
        Group {
            if app.hasProfile, let model {
                NavigationSplitView(columnVisibility: $columnVisibility) {
                    List(panes(for: model), selection: selection) { pane in
                        Label(pane.title, systemImage: pane.symbol)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(.rect)
                            // A click on the section already picked changes no selection, yet scrolls back to it
                            .simultaneousGesture(TapGesture().onEnded { pick(pane) })
                            .tag(pane)
                    }
                    .navigationSplitViewColumnWidth(min: 180, ideal: 220, max: 280)
                } detail: {
                    Group {
                        if pane == .categories {
                            MacCategoryMapPane()
                        } else {
                            ProfileView(model: model, pane: pane, scrollRequest: scrollRequest)
                        }
                    }
                    // The sidebar's button at the detail's leading edge, right of the divider, where the main
                    // window's sits (Pierre's beta feedback), rather than in the sidebar's own header
                    .toolbar {
                        ToolbarItem(placement: .navigation) {
                            SidebarToggleButton(visibility: $columnVisibility)
                        }
                    }
                }
                .hidingSystemSidebarToggle(unifiesToolbar: true)
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
                case .profilePhoto: ProfilePhotoView()
                case .deleteAccount: DeleteAccountView()
                case let .inAppUpdate(route): InAppUpdateView(route: route)
                case let .appDownload(route): AppDownloadView(route: route)
                default: EmptyView()
                }
            }
        }
        .tint(Palette.accent)
        // Closing the window closes its sheet, which the main window waits on
        .onDisappear { app.settingsSheet = nil }
        // The profile or the category map opened from anywhere picks its section, also as the window opens
        .onChange(of: app.requestedSettingsPane, initial: true) { _, requested in
            guard let requested else { return }
            pick(requested)
            app.requestedSettingsPane = nil
        }
        .onChange(of: app.hasProfile, initial: true) { _, hasProfile in
            if hasProfile {
                if model == nil { model = ProfileModel() }
            } else {
                model = nil
                dismiss()
            }
        }
    }

    /// The sidebar's selection, moved by a click or the arrow keys.
    private var selection: Binding<ProfileSettingsPane?> {
        Binding(
            get: { pane },
            set: { newPane in
                guard newPane != pane, let newPane else { return }
                pick(newPane)
            },
        )
    }

    /// iPhone only where the invite exists: once the App Store lists the app.
    private func panes(for model: ProfileModel) -> [ProfileSettingsPane] {
        ProfileSettingsPane.allCases.filter { $0 != .iphone || model.state.appInvite != nil }
    }

    private func pick(_ newPane: ProfileSettingsPane) {
        pane = newPane
        scrollRequest += 1
    }
}
#endif
