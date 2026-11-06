import Shared
import SwiftUI

/// The first access flow lives in one NavigationStack; once a profile exists the shared back stack is replaced by
/// Início, which brings the tabs (or the sidebar on iPad and Mac): every other tab is the entry right above it, and each
/// tab pushes what it opens (the profile on iPhone and iPad, a group) inside its own stack. A route that opens as a
/// sheet (a key, a bill or Edit profile with an iPad's room, deleting the account, a new group, its
/// invite on an iPhone and the Mac, editing a group, a new or edited bill, recording a payment, Contacts' invite, the
/// Mac's invite opened from its link) shows over the whole window, and a sheet opened from a sheet stacks over it; the
/// Mac's invite from a group is a popover on the group's own button, and the Mac's bill the window's inspector. The root measures the window, which every screen
/// reads as `windowLayout`; only crossing an iPad's room changes what opens as a sheet and redraws the root. The Mac
/// never draws the profile here: opening it opens Ajustes (⌘,), the app's Settings window (`SettingsView`), where its
/// key and account deletion show as sheets; while one is open this window takes no clicks, since both draw the one
/// back stack.
struct RootView: View {
    /// Owned by the app, which the Mac's Settings window shares.
    @Bindable var app: AppModel

    #if os(macOS)
    @Environment(\.openSettings) private var openSettings
    #endif

    var body: some View {
        Group {
            if !app.isReady || !app.isWindowMeasured {
                // The start route depends on whether a profile exists, read from Room in a moment, and what opens
                // as a sheet on the window's size, measured on the first layout pass
                Color.clear
            } else if app.root == .home {
                MainTabView(path: $app.path)
                    #if os(macOS)
                    // Conta beside what opened it, which still takes clicks (decision 3)
                    .inspector(isPresented: isBillInspectorPresented) {
                        if let route = app.billInspector {
                            BillDetailView(route: route)
                                .id(route)
                        }
                    }
                    #endif
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
            // A bill open as a sheet with an iPad's room is edited in a sheet over it
            .sheet(item: $app.stackedSheet) { route in
                NavigationStack(path: $app.stackedSheetPath) {
                    screen(for: route)
                        .navigationDestination(for: AppRoute.self) { route in
                            screen(for: route)
                        }
                }
                .rootSheetSizing(for: route, pushed: app.stackedSheetPath)
            }
            // The front half of an iPhone Duo open, with what it opened over in the other one; pushing Divisão padrão
            // grows Editar grupo's form sheet to a bill's, and popping it shrinks it back
            .rootSheetSizing(for: route, pushed: app.sheetPath)
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
        // A comment's link this device can't open: a group the person isn't in, or no internet to find out
        .alert(
            app.billLinkNotice == .offline ? Text("No internet") : Text("You're not in this group"),
            isPresented: isBillLinkNoticePresented,
        ) {
            Button("Go to Home") {
                app.dismissBillLinkNotice()
            }
        } message: {
            if app.billLinkNotice == .offline {
                Text("Open the link again once you're back online.")
            } else {
                Text("This comment is in a group you're not in. Ask whoever sent you the link for the invite.")
            }
        }
        .onChange(of: app.theme, initial: true) { _, theme in
            WindowAppearance.apply(theme)
        }
        #if os(macOS)
        .onChange(of: app.settingsRequestCount) {
            openSettings()
        }
        // Ajustes' key or Excluir conta sits on top of the back stack this window also draws
        .disabled(app.settingsSheet != nil)
        // The mouse's back and forward buttons, and Go › Back and Forward (⌘[ and ⌘])
        .mouseNavigationButtons(back: { app.goBack() }, forward: { app.goForward() })
        // The window's sidebar button sits at the detail's leading edge (`MainTabView`), never in the sidebar's header
        .hidingSystemSidebarToggle()
        .focusedSceneValue(\.windowNavigation, app)
        .environment(\.isWindowUncovered, app.isWindowUncovered)
        .frame(minWidth: 520, minHeight: 560)
        #endif
        // Outermost, so the sheets inherit the window the root measured
        .measuringWindow { app.windowMeasured($0) }
    }

    /// Closing the bill link's notice any way goes back to Início, as "Go to Home" does.
    private var isBillLinkNoticePresented: Binding<Bool> {
        Binding(
            get: { app.billLinkNotice != nil },
            set: { isPresented in
                if !isPresented { app.dismissBillLinkNotice() }
            },
        )
    }

    #if os(macOS)
    private var isBillInspectorPresented: Binding<Bool> {
        Binding(
            get: { app.billInspector != nil },
            set: { isPresented in
                if !isPresented { app.billInspector = nil }
            },
        )
    }
    #endif

    @ViewBuilder
    private func screen(for route: AppRoute) -> some View {
        switch route {
        case .welcome: WelcomeView()
        case .profileSetup: ProfileSetupView()
        case .restore: RestoreView()
        // Grupos is never the root now; a stack that starts there still draws the tabs
        case .home, .groups: MainTabView(path: $app.path)
        case .search: SearchView()
        case .createGroup: CreateGroupView()
        case let .invite(route): InviteView(route: route)
        case let .groupDetail(route): GroupDetailView(route: route)
        case let .registerPayment(route): RegisterPaymentView(route: route)
        case let .globalBalances(route): GlobalBalancesView(route: route)
        case let .activity(route): ActivityView(route: route)
        case let .settleUp(route): RegisterPaymentView(settleUp: route)
        case let .charge(route): ChargeView(route: route)
        case .pickPayment: PickPaymentView()
        case let .splitBill(route): SplitBillView(route: route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .shareBill(route): ShareBillView(route: route)
        case let .editGroup(route): EditGroupView(route: route)
        case let .defaultSplit(route): DefaultSplitView(route: route)
        case let .simplifyDebtsInfo(route): SimplifyDebtsInfoView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
        case let .categoryExpenses(route): CategoryExpensesView(route: route)
        case let .joinInvite(route): JoinInviteView(route: route)
        case let .joinPick(route): JoinPickView(route: route)
        case let .joinWithLink(route): JoinWithLinkView(route: route)
        // Contacts is never the root nor a sheet: the tabs draw it
        case .contacts: MainTabView(path: $app.path)
        case let .contactProfile(route): ContactProfileView(route: route)
        case .contactsAgenda: AgendaView()
        case let .contactInvite(route): ContactInviteView(route: route)
        case .profile:
            #if os(macOS)
            // The Mac's profile is Ajustes (⌘,), never pushed
            EmptyView()
            #else
            ProfileView()
            #endif
        case .editProfile: EditProfileView()
        case .profilePhoto: ProfilePhotoView()
        case .deleteAccount: DeleteAccountView()
        case .notificationSettings: NotificationSettingsView()
        case .categoryMap: CategoryMapView()
        case let .paymentKey(route): PaymentKeyView(route: route)
        case let .inAppUpdate(route): InAppUpdateView(route: route)
        case let .appDownload(route): AppDownloadView(route: route)
        }
    }
}

private extension View {
    /// The sheet at its route's size, or a bill's while a route that grows it is pushed inside it (Divisão padrão in
    /// Editar grupo's), placed by the iPhone Duo's fold: SwiftUI keeps the sizing it is given now, but only
    /// `resizingSheet` moves the open sheet to it.
    func rootSheetSizing(for route: AppRoute, pushed: [AppRoute]) -> some View {
        let isGrown = !route.usesBillSheet && pushed.contains { $0.growsSheetToBill }
        return foldSheetPlacement(isForm: route.usesFormSheet && !isGrown, isBill: route.usesBillSheet || isGrown)
            .resizingSheet(toBill: isGrown)
    }
}
