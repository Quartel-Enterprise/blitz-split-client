import SwiftUI

/// The first access flow lives in one NavigationStack; once a profile exists the shared back stack is replaced by
/// Início, which brings the tabs (or the sidebar on iPad and Mac): every other tab is the entry right above it, and each
/// tab pushes what it opens (the profile, a group) inside its own stack. A route that opens as a sheet (a key or a bill
/// with an iPad's room and on the Mac, deleting the account, a new group, its invite on an iPhone and the Mac, editing
/// a group, a new or edited bill, recording a payment, Contacts' invite, the Mac's invite opened from its link) shows
/// over the whole window, and a sheet opened from a sheet stacks over it; the Mac's invite from a group is a popover on
/// the group's own button. The root measures the window, which every screen reads as `windowLayout`; only crossing an
/// iPad's room changes what opens as a sheet and redraws the root.
struct RootView: View {
    @State private var app = AppModel()

    var body: some View {
        Group {
            if !app.isReady || !app.isWindowMeasured {
                // The start route depends on whether a profile exists, read from Room in a moment, and what opens
                // as a sheet on the window's size, measured on the first layout pass
                Color.clear
            } else if app.root == .home {
                MainTabView(path: $app.path)
            } else {
                NavigationStack(path: $app.path) {
                    screen(for: app.root)
                        .navigationDestination(for: AppRoute.self) { route in
                            screen(for: route)
                        }
                }
            }
        }
        .sheet(item: $app.sheet) { route in
            // Edit group pushes its default split inside its own sheet
            NavigationStack(path: $app.sheetPath) {
                screen(for: route)
                    .navigationDestination(for: AppRoute.self) { route in
                        screen(for: route)
                    }
            }
            // A bill open as a sheet (iPad, Mac) is edited in a sheet over it
            .sheet(item: $app.stackedSheet) { route in
                NavigationStack(path: $app.stackedSheetPath) {
                    screen(for: route)
                        .navigationDestination(for: AppRoute.self) { route in
                            screen(for: route)
                        }
                }
            }
        }
        #if os(iOS)
        .fullScreenCover(item: $app.cover) { route in
            screen(for: route)
        }
        #endif
        // The Mac's invite over a group's tab, drawn by that tab's "Invite people"
        .environment(\.invitePopover, app.invitePopover)
        .environment(\.dismissInvitePopover, { app.invitePopover = nil })
        .tint(Palette.accent)
        .onChange(of: app.theme, initial: true) { _, theme in
            WindowAppearance.apply(theme)
        }
        #if os(macOS)
        .frame(minWidth: 520, minHeight: 560)
        #endif
        // Outermost, so the sheets inherit the window the root measured
        .measuringWindow { app.windowMeasured($0) }
    }

    @ViewBuilder
    private func screen(for route: AppRoute) -> some View {
        switch route {
        case .welcome: WelcomeView()
        case .profileSetup: ProfileSetupView()
        case .restore: RestoreView()
        // Grupos is never the root now; a stack that starts there still draws the tabs
        case .home, .groups: MainTabView(path: $app.path)
        case .createGroup: CreateGroupView()
        case let .invite(route): InviteView(route: route)
        case let .members(route): MembersView(route: route)
        case let .groupDetail(route): GroupDetailView(route: route)
        case let .balances(route): BalancesView(route: route)
        case let .registerPayment(route): RegisterPaymentView(route: route)
        case let .globalBalances(route): GlobalBalancesView(route: route)
        case let .settleUp(route): RegisterPaymentView(settleUp: route)
        case .pickPayment: PickPaymentView()
        case let .splitBill(route): SplitBillView(route: route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .editGroup(route): EditGroupView(route: route)
        case let .defaultSplit(route): DefaultSplitView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
        case let .joinInvite(route): JoinInviteView(route: route)
        case let .joinPick(route): JoinPickView(route: route)
        case let .joinWithLink(route): JoinWithLinkView(route: route)
        // Contacts is never the root nor a sheet: the tabs draw it
        case .contacts: MainTabView(path: $app.path)
        case let .contactProfile(route): ContactProfileView(route: route)
        case .contactsAgenda: AgendaView()
        case let .contactInvite(route): ContactInviteView(route: route)
        case .profile: ProfileView()
        case .editProfile: EditProfileView()
        case .deleteAccount: DeleteAccountView()
        case .notificationSettings: NotificationSettingsView()
        case let .paymentKey(route): PaymentKeyView(route: route)
        }
    }
}
