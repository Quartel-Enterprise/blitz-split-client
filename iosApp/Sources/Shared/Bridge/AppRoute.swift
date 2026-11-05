import Shared

/// Swift's view of the shared back stack entries: Hashable, so a NavigationStack path can hold it.
enum AppRoute: Hashable, Identifiable {
    case welcome
    case profileSetup
    case restore
    /// Início, the home's first tab and the root of the back stack once there is a profile.
    case home
    /// Grupos, the home's tab right above Início.
    case groups
    /// "Search" over Home, Balances, Contacts or Activity: pushed on an iPhone, a sheet on the Mac.
    case search
    case createGroup
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case invite(RouteInvite)
    /// The group's screen, Expenses, Balances and Members under its header; holds the Kotlin route itself, which its
    /// ViewModel reads the group and the tab it opens on from.
    case groupDetail(RouteGroupDetail)
    /// Holds the Kotlin route itself, which its ViewModel reads the group and the pair from.
    case registerPayment(RouteRegisterPayment)
    /// Início's "Who paid whom?", before Record payment takes its place.
    case pickPayment
    /// The home's Balances tab; holds the Kotlin route itself, which its ViewModel reads the filter from.
    case globalBalances(RouteGlobalBalances)
    /// Activity, pushed over Home by its bell or over a contact's profile; holds the Kotlin route itself, which its
    /// ViewModel reads the person it filters by from.
    case activity(RouteActivity)
    /// Settling up with someone across groups; holds the Kotlin route itself, which its ViewModel reads the person from.
    case settleUp(RouteSettleUp)
    /// Request, over Balances or a contact's profile; holds the Kotlin route itself, which its ViewModel reads the
    /// person and the currency from.
    case charge(RouteCharge)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case splitBill(RouteSplitBill)
    /// Holds the Kotlin route itself, which its ViewModel reads the group and the bill from.
    case billDetail(RouteBillDetail)
    /// Share bill, over the bill; holds the Kotlin route itself, which its ViewModel reads the group and the bill from.
    case shareBill(RouteShareBill)
    /// Holds the Kotlin route itself, which its ViewModel reads the group from.
    case editGroup(RouteEditGroup)
    /// The group's default split, pushed inside Edit group's sheet; holds the Kotlin route itself, which its ViewModel
    /// reads the group from.
    case defaultSplit(RouteDefaultSplit)
    /// "How it works" of "Simplify debts", pushed inside Edit group's sheet or over the group; holds the Kotlin route
    /// itself, whose currency the example's amounts are in.
    case simplifyDebtsInfo(RouteSimplifyDebtsInfo)
    /// The group's statistics; holds the Kotlin route itself, which its ViewModel reads the group from.
    case groupStats(RouteGroupStats)
    /// A category's bills, from the statistics; holds the Kotlin route itself, which its ViewModel reads the group,
    /// the category and the period from.
    case categoryExpenses(RouteCategoryExpenses)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinInvite(RouteJoinInvite)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite code from.
    case joinPick(RouteJoinPick)
    /// Holds the Kotlin route itself, which its ViewModel reads the invite to skip from.
    case joinWithLink(RouteJoinWithLink)
    /// The home's Contacts tab, right above the groups.
    case contacts
    /// A person's profile; holds the Kotlin route itself, which its ViewModel reads the person from.
    case contactProfile(RouteContactProfile)
    /// Inviting from the device's contacts.
    case contactsAgenda
    /// A group's invite link for someone or anyone; holds the Kotlin route itself, which its ViewModel reads whom it
    /// is for from.
    case contactInvite(RouteContactInvite)
    case profile
    case editProfile
    /// The profile's photo big, with the shortcuts that change it (Bible Planner's viewer).
    case profilePhoto
    case deleteAccount
    case notificationSettings
    case categoryMap
    /// Holds the Kotlin route itself, which its ViewModel reads the method and the key from.
    case paymentKey(RoutePaymentKey)
    /// "Update available" over wherever the app is; holds the Kotlin route itself, which its ViewModel reads the
    /// App Store's version and page from.
    case inAppUpdate(RouteInAppUpdate)

    init(_ route: any Route) {
        switch route {
        case is RouteProfileSetup: self = .profileSetup
        case is RouteRestore: self = .restore
        case is RouteHome: self = .home
        case is RouteGroups: self = .groups
        case is RouteSearch: self = .search
        case is RouteCreateGroup: self = .createGroup
        case let invite as RouteInvite: self = .invite(invite)
        case let groupDetail as RouteGroupDetail: self = .groupDetail(groupDetail)
        case let registerPayment as RouteRegisterPayment: self = .registerPayment(registerPayment)
        case is RoutePickPayment: self = .pickPayment
        case let globalBalances as RouteGlobalBalances: self = .globalBalances(globalBalances)
        case let activity as RouteActivity: self = .activity(activity)
        case let settleUp as RouteSettleUp: self = .settleUp(settleUp)
        case let charge as RouteCharge: self = .charge(charge)
        case let splitBill as RouteSplitBill: self = .splitBill(splitBill)
        case let billDetail as RouteBillDetail: self = .billDetail(billDetail)
        case let shareBill as RouteShareBill: self = .shareBill(shareBill)
        case let editGroup as RouteEditGroup: self = .editGroup(editGroup)
        case let defaultSplit as RouteDefaultSplit: self = .defaultSplit(defaultSplit)
        case let simplifyDebtsInfo as RouteSimplifyDebtsInfo: self = .simplifyDebtsInfo(simplifyDebtsInfo)
        case let groupStats as RouteGroupStats: self = .groupStats(groupStats)
        case let categoryExpenses as RouteCategoryExpenses: self = .categoryExpenses(categoryExpenses)
        case let joinInvite as RouteJoinInvite: self = .joinInvite(joinInvite)
        case let joinPick as RouteJoinPick: self = .joinPick(joinPick)
        case let joinWithLink as RouteJoinWithLink: self = .joinWithLink(joinWithLink)
        case is RouteContacts: self = .contacts
        case let contactProfile as RouteContactProfile: self = .contactProfile(contactProfile)
        case is RouteContactsAgenda: self = .contactsAgenda
        case let contactInvite as RouteContactInvite: self = .contactInvite(contactInvite)
        case is RouteProfile: self = .profile
        case is RouteEditProfile: self = .editProfile
        case is RouteProfilePhoto: self = .profilePhoto
        case is RouteDeleteAccount: self = .deleteAccount
        case is RouteNotificationSettings: self = .notificationSettings
        case is RouteCategoryMap: self = .categoryMap
        case let paymentKey as RoutePaymentKey: self = .paymentKey(paymentKey)
        case let inAppUpdate as RouteInAppUpdate: self = .inAppUpdate(inAppUpdate)
        default: self = .welcome
        }
    }

    var id: Self { self }

    /// With an iPad's room (`isLargeWindow`, the window's `ScreenLayout.isLarge`: an iPad, an iPhone Duo open) a key,
    /// a bill and Edit profile open as a form sheet and on the Mac a key as a sheet (on Ajustes' window,
    /// `opensInSettings`; the Mac edits the profile inline, so never Edit profile, and shows a bill in the window's
    /// inspector), as the canvas draws them; an iPhone, lying down too, pushes them. Deleting the account, a new group, editing a group, a new or edited bill,
    /// recording a payment (settling up across groups and Início's "Who paid whom?" included), Request, Contacts' invite and
    /// sharing a bill are sheets everywhere (sharing stacks over the bill's own sheet with an iPad's room, and over the
    /// Mac's inspector). The invite is a sheet on an iPhone and is pushed full screen with an iPad's room (decision 1);
    /// on the Mac it is a sheet, or the popover of the group behind it (`AppModel.invitePopover`). The invite reader is
    /// a sheet on the Mac, and covers the whole screen on iPhone and iPad. An invite opened from its link is one sheet
    /// on the Mac, "Who are you" pushed inside it. Search over a tab other than Groups is a sheet on the Mac and pushed
    /// on an iPhone.
    func presentsAsSheet(isLargeWindow: Bool) -> Bool {
        if isSheetEverywhere { return true }
        switch self {
        case .invite:
            #if os(macOS)
            return true
            #else
            return !isLargeWindow
            #endif
        // "How it works" from the group is a sheet on the Mac, pushed inside Edit group's sheet everywhere
        case .joinWithLink, .joinInvite, .search, .simplifyDebtsInfo:
            #if os(macOS)
            return true
            #else
            return false
            #endif
        case .paymentKey, .editProfile:
            #if os(macOS)
            return true
            #else
            return isLargeWindow
            #endif
        // The Mac draws the bill in the main window's inspector (`AppModel.billInspector`)
        case .billDetail:
            #if os(macOS)
            return false
            #else
            return isLargeWindow
            #endif
        default:
            return false
        }
    }

    /// The screens that are a sheet on every device and window size (the list `presentsAsSheet` opens with).
    private var isSheetEverywhere: Bool {
        switch self {
        case .deleteAccount, .createGroup, .splitBill, .editGroup, .registerPayment, .settleUp, .charge, .pickPayment,
             .contactInvite, .shareBill, .inAppUpdate: true
        default: false
        }
    }

    /// A screen no tab ever pushes as a page: a sheet everywhere, or the invite reader and the photo big, which cover
    /// the whole screen on iPhone and iPad and are sheets on the Mac. Under a screen pushed over it (`AppModel.path`)
    /// it hides instead of showing as a page.
    var drawsOnlyAsModal: Bool {
        switch self {
        case .joinWithLink, .profilePhoto: true
        default: isSheetEverywhere
        }
    }

    /// Pushed inside the sheet under it rather than over the window: the default split and "How it works" of Simplify
    /// debts inside Edit group's sheet, the category map inside Split a bill's (9.0), and on the Mac "Who are you"
    /// inside the invite's. From the profile the
    /// map is pushed as any screen; the Mac never pushes it, since it lives in Ajustes (`AppModel`).
    var pushesInSheet: Bool {
        switch self {
        case .defaultSplit, .categoryMap, .simplifyDebtsInfo:
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

    /// What only the profile opens, which on the Mac lives in Ajustes (⌘,): a key, the photo big, deleting the
    /// account and the update "Check for updates" found show as sheets on the Settings window there
    /// (`AppModel.settingsSheet`). Never off the Mac.
    var opensInSettings: Bool {
        #if os(macOS)
        switch self {
        case .paymentKey, .profilePhoto, .deleteAccount: true
        // "Check for updates" is Ajustes' own; opening the app offers the update over the main window
        case let .inAppUpdate(route): route.source == .manual
        default: false
        }
        #else
        false
        #endif
    }

    /// A screen drawn as a form sheet (`formSheetSizing()`), which the root's sheet keeps at the form's size, the
    /// iPhone Duo's fold included (`foldSheetPlacement(isForm:)`).
    var usesFormSheet: Bool {
        switch self {
        case .createGroup, .contactInvite, .shareBill, .editProfile, .editGroup, .registerPayment, .settleUp,
             .charge, .pickPayment, .inAppUpdate: true
        default: false
        }
    }

    /// A bill's screen (Rachar conta, Conta), which the root's sheet keeps at the bill's size with an iPad's room
    /// (`billSheetSizing(in:)`), the iPhone Duo's fold included (`foldSheetPlacement(isBill:)`).
    var usesBillSheet: Bool {
        switch self {
        case .splitBill, .billDetail: true
        default: false
        }
    }

    /// Pushed inside a form sheet, grows it to a bill's size with an iPad's room until it is popped: Divisão padrão
    /// inside Editar grupo's, as the boards draw it.
    var growsSheetToBill: Bool {
        switch self {
        case .defaultSplit: true
        default: false
        }
    }

    /// A group's screen, whatever tab it shows: the screen that roots the column beside the list, and the one whose
    /// "Invite people" (Members' Share) anchors the Mac's invite popover.
    var isGroupScreen: Bool {
        if case .groupDetail = self { return true }
        return false
    }

    /// The invite reader's camera, which stays dark in both themes, and the profile's photo big over the blurred
    /// profile, over the whole screen of an iPhone or iPad.
    var presentsFullScreen: Bool {
        #if os(iOS)
        switch self {
        case .joinWithLink, .profilePhoto: return true
        default: return false
        }
        #else
        return false
        #endif
    }
}
