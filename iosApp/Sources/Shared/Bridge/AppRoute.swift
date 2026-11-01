import Shared

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
    /// The group's default split, pushed inside Edit group's sheet; holds the Kotlin route itself, which its ViewModel
    /// reads the group from.
    case defaultSplit(RouteDefaultSplit)
    /// The group's statistics; holds the Kotlin route itself, which its ViewModel reads the group from.
    case groupStats(RouteGroupStats)
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
        case let defaultSplit as RouteDefaultSplit: self = .defaultSplit(defaultSplit)
        case let groupStats as RouteGroupStats: self = .groupStats(groupStats)
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

    /// With an iPad's room (`isLargeWindow`, the window's `ScreenLayout.isLarge`: an iPad, an iPhone Duo open) a key,
    /// a bill and Edit profile open as a form sheet and on the Mac as a sheet (a key on Ajustes' window,
    /// `opensInSettings`; the Mac edits the profile inline, so never Edit profile), as the canvas draws them; an iPhone,
    /// lying down too, pushes them. Deleting the account, a new group, editing a group, a new or edited bill and
    /// recording a payment (settling up across groups included) are sheets everywhere. The invite is a sheet on an
    /// iPhone and is pushed full screen with an iPad's room (decision 1); on the Mac it is a sheet, or the popover of
    /// the group behind it (`AppModel.invitePopover`). The invite reader is a sheet on the Mac, and covers the whole
    /// screen on iPhone and iPad. An invite opened from its link is one sheet on the Mac, "Who are you" pushed inside it.
    func presentsAsSheet(isLargeWindow: Bool) -> Bool {
        switch self {
        case .deleteAccount, .createGroup, .splitBill, .editGroup, .registerPayment, .settleUp:
            return true
        case .invite:
            #if os(macOS)
            return true
            #else
            return !isLargeWindow
            #endif
        case .joinWithLink, .joinInvite:
            #if os(macOS)
            return true
            #else
            return false
            #endif
        case .paymentKey, .billDetail, .editProfile:
            #if os(macOS)
            return true
            #else
            return isLargeWindow
            #endif
        default:
            return false
        }
    }

    /// Pushed inside the sheet under it rather than over the window: the default split inside Edit group's sheet, and
    /// on the Mac "Who are you" inside the invite's.
    var pushesInSheet: Bool {
        switch self {
        case .defaultSplit:
            return true
        case .joinPick:
            #if os(macOS)
            return true
            #else
            return false
            #endif
        default:
            return false
        }
    }

    /// What only the profile opens, which on the Mac lives in Ajustes (⌘,): a key and deleting the account show as
    /// sheets on the Settings window there (`AppModel.settingsSheet`). Never off the Mac.
    var opensInSettings: Bool {
        #if os(macOS)
        switch self {
        case .paymentKey, .deleteAccount: true
        default: false
        }
        #else
        false
        #endif
    }

    /// A group's Expenses, Balances or Members: the tabs whose "Invite people" anchors the Mac's invite popover.
    var isGroupTab: Bool {
        switch self {
        case .groupDetail, .balances, .members: true
        default: false
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
