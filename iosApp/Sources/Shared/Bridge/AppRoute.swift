import Shared

#if os(iOS)
import UIKit
#endif

/// Swift's view of the shared back stack entries: Hashable, so a NavigationStack path can hold it.
enum AppRoute: Hashable, Identifiable {
    case welcome
    case profileSetup
    case restore
    case groups
    case createGroup
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case invite(RouteInvite)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case members(RouteMembers)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinInvite(RouteJoinInvite)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinPick(RouteJoinPick)
    case joinWithLink
    case profile
    case editProfile
    case deleteAccount
    case notificationSettings
    /// Holds the Kotlin route itself, which its ViewModel reads the method and the key from.
    case paymentKey(RoutePaymentKey)

    init(_ route: any Route) {
        switch route {
        case is RouteProfileSetup: self = .profileSetup
        case is RouteRestore: self = .restore
        case is RouteGroups: self = .groups
        case is RouteCreateGroup: self = .createGroup
        case let invite as RouteInvite: self = .invite(invite)
        case let members as RouteMembers: self = .members(members)
        case let joinInvite as RouteJoinInvite: self = .joinInvite(joinInvite)
        case let joinPick as RouteJoinPick: self = .joinPick(joinPick)
        case is RouteJoinWithLink: self = .joinWithLink
        case is RouteProfile: self = .profile
        case is RouteEditProfile: self = .editProfile
        case is RouteDeleteAccount: self = .deleteAccount
        case is RouteNotificationSettings: self = .notificationSettings
        case let paymentKey as RoutePaymentKey: self = .paymentKey(paymentKey)
        default: self = .welcome
        }
    }

    var id: Self { self }

    /// iPad opens a key as a form sheet and the Mac as a sheet on its window, as the canvas draws
    /// them; the iPhone pushes it. Deleting the account, a new group and its invite are sheets everywhere.
    /// The invite reader is a sheet on the Mac, and covers the whole screen on iPhone and iPad.
    var presentsAsSheet: Bool {
        switch self {
        case .deleteAccount, .createGroup, .invite:
            return true
        case .joinWithLink:
            #if os(macOS)
            return true
            #else
            return false
            #endif
        case .paymentKey:
            #if os(macOS)
            return true
            #else
            return UIDevice.current.userInterfaceIdiom == .pad
            #endif
        default:
            return false
        }
    }

    /// The invite reader's camera, which stays dark in both themes, over the whole screen of an iPhone or iPad.
    var presentsFullScreen: Bool {
        #if os(iOS)
        return self == .joinWithLink
        #else
        return false
        #endif
    }
}
