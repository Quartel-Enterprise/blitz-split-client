import Foundation
import Shared
import SwiftUI

/// The words of the home's groups, the same on iPhone, iPad and Mac: the shared code hands enums and amounts, and
/// these pick the text.
enum GroupsCopy {
    /// "6 members" or "Just you".
    static func members(_ count: Int32) -> String {
        count == 1 ? String(localized: "Just you", bundle: .appLanguage) : String(
            localized: "\(Int(count)) members",
            bundle: .appLanguage,
        )
    }

    /// The Mac's Members column: "6 · Pierre, Ana, Bruno and 3 more", or "Just you".
    static func memberNames(_ row: GroupRow) -> String {
        guard row.memberCount > 1 else { return String(localized: "Just you", bundle: .appLanguage) }
        var names = row.memberNames
        if row.extraMemberCount > 0 {
            names.append(String(localized: "\(Int(row.extraMemberCount)) more", bundle: .appLanguage))
        }
        return "\(row.memberCount) · \(ExpenseFormat.names(names))"
    }

    /// Under a row's name: "today", "yesterday", "3 days", "1 week" or "Oct 3".
    static func activity(_ activity: LastActivity) -> String {
        switch activity.age {
        case .today: String(
            localized: "today",
            bundle: .appLanguage,
            comment: "A group's last activity, under its name on the home",
        )
        case .yesterday: String(
            localized: "yesterday",
            bundle: .appLanguage,
            comment: "A group's last activity, under its name on the home",
        )
        case .days: String(localized: "\(Int(activity.count)) days", bundle: .appLanguage)
        case .weeks: String(localized: "\(Int(activity.count)) weeks", bundle: .appLanguage)
        default: shortDate(activity.epochMillis)
        }
    }

    /// The Mac's Last activity column: "Today", "Yesterday", "3 days ago", "1 week ago" or "Oct 3".
    static func activitySentence(_ activity: LastActivity) -> String {
        switch activity.age {
        case .today: String(localized: "Today", bundle: .appLanguage)
        case .yesterday: String(localized: "Yesterday", bundle: .appLanguage)
        case .days: String(localized: "\(Int(activity.count)) days ago", bundle: .appLanguage)
        case .weeks: String(localized: "\(Int(activity.count)) weeks ago", bundle: .appLanguage)
        default: shortDate(activity.epochMillis)
        }
    }

    /// "queued · Groceries", "you get 120.00 and owe 218.00", "others still settling up", "invite someone",
    /// "6 members · everyone settled up on Oct 3", or else the last activity.
    static func detail(_ row: GroupRow) -> String {
        switch row.detail {
        case .queued:
            if let bill = row.pendingBillDescription { return String(
                localized: "queued · \(bill)",
                bundle: .appLanguage,
            ) }
            return String(localized: "queued", bundle: .appLanguage)
        case .mixed:
            let receives = row.currency.formatPlain(row.receivesMinor)
            let owes = row.currency.formatPlain(row.owesMinor)
            return String(localized: "you get \(receives) and owe \(owes)", bundle: .appLanguage)
        case .othersStillSettling: return String(localized: "others still settling up", bundle: .appLanguage)
        case .inviteSomeone: return String(localized: "invite someone", bundle: .appLanguage)
        case .everyoneSettled:
            let date = shortDate(row.lastActivity.epochMillis)
            return String(
                localized: "\(members(row.memberCount)) · everyone settled up on \(date)",
                bundle: .appLanguage,
            )
        default: return activity(row.lastActivity)
        }
    }

    /// The Mac's Last activity column: when, then what the row would say under its name.
    static func activityColumn(_ row: GroupRow) -> String {
        let when = activitySentence(row.lastActivity)
        switch row.detail {
        case .activity: return when
        case .everyoneSettled:
            return String(
                localized: "Everyone settled up on \(shortDate(row.lastActivity.epochMillis))",
                bundle: .appLanguage,
            )
        default: return "\(when) · \(detail(row))"
        }
    }

    /// The word after the balance: "to get back", "you owe", "you're settled up", "no expenses" or "Settled up".
    static func balanceLabel(_ kind: GroupBalanceKind) -> String {
        switch kind {
        case .toReceive: String(localized: "to get back", bundle: .appLanguage)
        case .youOwe: String(localized: "you owe", bundle: .appLanguage)
        case .youAreSettled: String(localized: "you're settled up", bundle: .appLanguage)
        case .noExpenses: String(localized: "no expenses", bundle: .appLanguage)
        default: String(localized: "Settled up", bundle: .appLanguage)
        }
    }

    /// "to get back, across your groups", "to pay, across your groups" or "you're settled up".
    static func summaryHint(_ kind: GroupBalanceKind) -> String {
        switch kind {
        case .toReceive: String(localized: "to get back, across your groups", bundle: .appLanguage)
        case .youOwe: String(localized: "to pay, across your groups", bundle: .appLanguage)
        default: String(localized: "you're settled up", bundle: .appLanguage)
        }
    }

    /// The sync status, only when it asks for attention: "Offline · 1 change queued", "Syncing…" ("Syncing… 3 of 7
    /// groups" while a first download knows how many are coming) or "No response from the server".
    static func sync(
        _ badge: SyncBadge?,
        pendingChangeCount: Int32,
        downloadProgress: DownloadProgress? = nil,
    ) -> String? {
        // Offline while the account's first download waits: what still comes says so, in the badge's place
        if downloadProgress?.isWaitingForInternet == true { return syncing(downloadProgress) }
        return switch badge {
        case nil: nil
        case .offline:
            pendingChangeCount > 0
                ? String(localized: "Offline · \(Int(pendingChangeCount)) changes queued", bundle: .appLanguage)
                : String(localized: "Offline · everything is saved on your \(DeviceName.current)", bundle: .appLanguage)
        case .failed: String(localized: "No response from the server", bundle: .appLanguage)
        default: syncing(downloadProgress)
        }
    }

    /// Each currency's amount with its sign ("+R$ 186,40"), or a zero in the first currency when none has any.
    static func amounts(_ totals: [BalanceTotal], _ select: (BalanceTotal) -> Int64) -> [String] {
        let amounts = totals.filter { select($0) != 0 }.map { $0.currency.formatSigned(select($0)) }
        if !amounts.isEmpty { return amounts }
        return totals.first.map { [$0.currency.format(0)] } ?? []
    }

    /// "Oct 3" or "3 de out.".
    private static func shortDate(_ millis: Int64) -> String {
        ExpenseFormat.date(of: millis).formatted(.dateTime.day().month(.abbreviated))
    }
}
