import Shared

/// Swift's view of the shared back stack entries: Hashable, so a NavigationStack path can hold it.
enum AppRoute: Hashable {
    case welcome
    case profileSetup
    case restore
    case groups
    case profile
    case editProfile

    init(_ route: any Route) {
        switch route {
        case is RouteProfileSetup: self = .profileSetup
        case is RouteRestore: self = .restore
        case is RouteGroups: self = .groups
        case is RouteProfile: self = .profile
        case is RouteEditProfile: self = .editProfile
        default: self = .welcome
        }
    }
}
