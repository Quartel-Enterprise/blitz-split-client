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
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case groupDetail(RouteGroupDetail)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case balances(RouteBalances)
    /// Holds the Kotlin route itself, which its ViewModel reads the group and the pair from.
    case registerPayment(RouteRegisterPayment)
    /// The home's Balances tab; holds the Kotlin route itself, which its ViewModel reads the filter from.
    case globalBalances(RouteGlobalBalances)
    /// Settling up with someone across groups; holds the Kotlin route itself, which its ViewModel reads the person from.
    case settleUp(RouteSettleUp)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case splitBill(RouteSplitBill)
    /// Holds the Kotlin route itself, which its ViewModel reads the group and the bill from.
    case billDetail(RouteBillDetail)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case editGroup(RouteEditGroup)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinInvite(RouteJoinInvite)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinPick(RouteJoinPick)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite to skip from.
    case joinWithLink(RouteJoinWithLink)
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
        case let groupDetail as RouteGroupDetail: self = .groupDetail(groupDetail)
        case let balances as RouteBalances: self = .balances(balances)
        case let registerPayment as RouteRegisterPayment: self = .registerPayment(registerPayment)
        case let globalBalances as RouteGlobalBalances: self = .globalBalances(globalBalances)
        case let settleUp as RouteSettleUp: self = .settleUp(settleUp)
        case let splitBill as RouteSplitBill: self = .splitBill(splitBill)
        case let billDetail as RouteBillDetail: self = .billDetail(billDetail)
        case let editGroup as RouteEditGroup: self = .editGroup(editGroup)
        case let joinInvite as RouteJoinInvite: self = .joinInvite(joinInvite)
        case let joinPick as RouteJoinPick: self = .joinPick(joinPick)
        case let joinWithLink as RouteJoinWithLink: self = .joinWithLink(joinWithLink)
        case is RouteProfile: self = .profile
        case is RouteEditProfile: self = .editProfile
        case is RouteDeleteAccount: self = .deleteAccount
        case is RouteNotificationSettings: self = .notificationSettings
        case let paymentKey as RoutePaymentKey: self = .paymentKey(paymentKey)
        default: self = .welcome
        }
    }

    var id: Self { self }

    /// iPad opens a key and a bill as a form sheet and the Mac as a sheet on its window, as the canvas draws
    /// them; the iPhone pushes them. Deleting the account, a new group, its invite, editing a group, a new or
    /// edited bill and recording a payment (settling up across groups included) are sheets everywhere.
    /// The invite reader is a sheet on the Mac, and covers the whole screen on iPhone and iPad.
    var presentsAsSheet: Bool {
        switch self {
        case .deleteAccount, .createGroup, .invite, .splitBill, .editGroup, .registerPayment, .settleUp:
            return true
        case .joinWithLink:
            #if os(macOS)
            return true
            #else
            return false
            #endif
        case .paymentKey, .billDetail:
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
        if case .joinWithLink = self { return true }
        return false
        #else
        return false
        #endif
    }
}
