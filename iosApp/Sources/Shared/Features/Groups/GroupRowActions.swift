import Shared
import SwiftUI

/// "Invite people", "Leave group" and "Delete group" of a home's row (5.3, 5.4), the same in the ⋯ menu at its end
/// and in its long press (the context menu on iOS, the right click on the Mac). Offline, Leave and Delete say they
/// need the internet; online, what blocks them shows before the tap, which still opens the dialog listing what is
/// open. The Mac's items that open a dialog end in "…".
struct GroupRowActions: View {
    let group: GroupRow
    let isOnline: Bool
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        Button {
            send(GroupsUiEventOnInviteClicked(groupId: group.id))
        } label: {
            Label {
                Text("Invite people")
                Text("Group link or QR code")
            } icon: {
                Image(systemName: "person.badge.plus")
            }
        }
        Divider()
        Button {
            send(GroupsUiEventOnLeaveGroupClicked(groupId: group.id))
        } label: {
            Label {
                Text(leaveTitle)
                if let leaveHint {
                    Text(verbatim: leaveHint)
                }
            } icon: {
                Image(systemName: "rectangle.portrait.and.arrow.right")
            }
        }
        .disabled(!isOnline)
        Button(role: .destructive) {
            send(GroupsUiEventOnDeleteGroupClicked(groupId: group.id))
        } label: {
            Label {
                Text(deleteTitle)
                if let deleteHint {
                    Text(verbatim: deleteHint)
                }
            } icon: {
                Image(systemName: "trash")
            }
        }
        .disabled(!isOnline)
    }

    /// "Settle the R$ 98,00 first" while this person still owes or is owed, "Needs internet" offline.
    private var leaveHint: String? {
        if !isOnline { return String(localized: "Needs internet") }
        guard group.settleBeforeLeavingMinor > 0 else { return nil }
        return String(localized: "Settle the \(group.currency.format(group.settleBeforeLeavingMinor)) first")
    }

    /// "Only once everyone has settled up" while anybody in the group owes anybody, "Needs internet" offline.
    private var deleteHint: String? {
        if !isOnline { return String(localized: "Needs internet") }
        return group.hasOpenDebts ? String(localized: "Only once everyone has settled up") : nil
    }

    private var leaveTitle: LocalizedStringKey {
        #if os(macOS)
        "Leave group…"
        #else
        "Leave group"
        #endif
    }

    private var deleteTitle: LocalizedStringKey {
        #if os(macOS)
        "Delete group…"
        #else
        "Delete group"
        #endif
    }
}

/// The ⋯ at the end of a row, 44 points wide, so nobody depends on the long press.
struct GroupRowMenuButton: View {
    let group: GroupRow
    let isOnline: Bool
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        Menu {
            GroupRowActions(group: group, isOnline: isOnline, send: send)
        } label: {
            Image(systemName: "ellipsis")
                .frame(width: 44, height: 44)
                .contentShape(.rect)
        }
        .menuStyle(.button)
        .buttonStyle(.borderless)
        #if os(macOS)
        .menuIndicator(.hidden)
        .fixedSize()
        #endif
        .foregroundStyle(.secondary)
        .accessibilityLabel(Text("More actions: \(group.name)"))
    }
}
