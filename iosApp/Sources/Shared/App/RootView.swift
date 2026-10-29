import SwiftUI

/// The first access flow lives in one NavigationStack; once a profile exists the shared back stack
/// is replaced by the groups, which bring the tabs (or the sidebar on iPad and Mac) and push the
/// profile inside the Groups tab. A route that opens as a sheet (a key on iPad and Mac, deleting the account, a new group, its invite) shows over
/// the whole window.
struct RootView: View {
    @State private var app = AppModel()

    var body: some View {
        Group {
            if !app.isReady {
                // The start route depends on whether a profile exists, read from Room in a moment
                Color.clear
            } else if app.root == .groups {
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
            NavigationStack {
                screen(for: route)
            }
        }
        .tint(Palette.accent)
        .onChange(of: app.theme, initial: true) { _, theme in
            WindowAppearance.apply(theme)
        }
        #if os(macOS)
        .frame(minWidth: 520, minHeight: 560)
        #endif
    }

    @ViewBuilder
    private func screen(for route: AppRoute) -> some View {
        switch route {
        case .welcome: WelcomeView()
        case .profileSetup: ProfileSetupView()
        case .restore: RestoreView()
        case .groups: MainTabView(path: $app.path)
        case .createGroup: CreateGroupView()
        case let .invite(route): InviteView(route: route)
        case .profile: ProfileView()
        case .editProfile: EditProfileView()
        case .deleteAccount: DeleteAccountView()
        case .notificationSettings: NotificationSettingsView()
        case let .paymentKey(route): PaymentKeyView(route: route)
        }
    }
}
