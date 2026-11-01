import Shared
import SwiftUI

/// The words of "Leave group" and "Delete group", the same on Expenses, Balances and the home.
enum GroupExitCopy {
    /// The errors a group's tab toasts: leaving or deleting that didn't reach the server, a change not saved.
    static func errorToasts(leave: Int, delete: Int, change: Int) -> [GlassToastMessage] {
        [
            GlassToastMessage(text: "Couldn't leave the group. Check your connection and try again.", trigger: leave),
            GlassToastMessage(text: "Couldn't delete the group. Check your connection and try again.", trigger: delete),
            GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: change),
        ]
    }

    static func title(_ dialog: any GroupExitDialog) -> String {
        switch dialog {
        case is GroupExitDialogLeaveBlocked:
            String(localized: "Settle up before leaving")
        case is GroupExitDialogDeleteBlocked:
            String(localized: "There are still open balances")
        case let leave as GroupExitDialogConfirmLeave:
            String(localized: "Leave “\(leave.groupName)”?")
        case let delete as GroupExitDialogConfirmDelete:
            String(localized: "Delete “\(delete.groupName)”?")
        default:
            ""
        }
    }

    /// An alert has no room for rows: each pair still open is a line under the message.
    static func message(_ dialog: any GroupExitDialog, currency: GroupCurrency?) -> String {
        switch dialog {
        case let blocked as GroupExitDialogLeaveBlocked:
            let message = String(
                localized: "You still have an open balance in “\(blocked.groupName)”. Leaving now would leave it unsettled for the others.",
            )
            return withDebts(message, blocked.debts, currency: currency)
        case let blocked as GroupExitDialogDeleteBlocked:
            let message = String(
                localized: "“\(blocked.groupName)” can only be deleted once everyone has settled up.",
            )
            // "Faltam 2 acertos:", as Android and the web count them, once there are pairs to list
            let count = blocked.debts.isEmpty ? "" : " " + String(localized: "\(blocked.debts.count) payments left:")
            return withDebts(message + count, blocked.debts, currency: currency)
        case let leave as GroupExitDialogConfirmLeave:
            if leave.deletesGroup {
                return String(localized: "Your balance is settled. Nobody else in the group has joined, so it's deleted for good.")
            }
            return String(
                localized: "Your balance is settled. The group leaves this device and your expenses keep your name for the other \(Int(leave.otherMemberCount)) members. To come back, someone has to invite you again.",
            )
        case let delete as GroupExitDialogConfirmDelete:
            let members = String(localized: "all \(Int(delete.memberCount)) members")
            if delete.billCount == 0 {
                return String(localized: "The group is gone for \(members). This can't be undone.")
            }
            let bills = String(localized: "its \(Int(delete.billCount)) expenses")
            return String(
                localized: "Everyone has settled up. The group, \(bills) and the history are gone for \(members). This can't be undone.",
            )
        default:
            return ""
        }
    }

    static func deleted(_ notice: GroupDeletedNotice) -> String {
        if let name = notice.deletedByName {
            return String(
                localized: "\(name) deleted “\(notice.groupName)” after everyone settled up. Nothing was left open, and the group leaves your list.",
            )
        }
        return String(
            localized: "“\(notice.groupName)” was deleted after everyone settled up. Nothing was left open, and the group leaves your list.",
        )
    }

    private static func withDebts(_ message: String, _ debts: [DebtRow], currency: GroupCurrency?) -> String {
        guard let currency, !debts.isEmpty else { return message }
        let lines = debts.map { "\(DebtCopy.sentence($0)): \(DebtCopy.amount($0, currency: currency))" }
        return ([message, ""] + lines).joined(separator: "\n")
    }
}

extension View {
    /// What "Leave group" and "Delete group" open (5.3c to 5.3f), and "Group deleted" (6.1k), over the group's tab.
    /// While the server answers, the alert gives way to a spinner over the screen.
    func groupExitDialogs(exit: GroupExitState?, currency: GroupCurrency?, actions: GroupScreenActions) -> some View {
        modifier(
            GroupExitDialogs(
                exit: exit,
                currency: currency,
                onConfirm: actions.confirmExit,
                onDismiss: actions.dismissExit,
                onBackToGroups: actions.backToGroups,
            ),
        )
    }
}

extension View {
    /// The same dialogs over the home, from a row's ⋯ menu or long press, which never shows "Group deleted".
    func groupExitDialogs(
        exit: GroupExitState,
        currency: GroupCurrency?,
        onConfirm: @escaping () -> Void,
        onDismiss: @escaping () -> Void,
    ) -> some View {
        modifier(
            GroupExitDialogs(
                exit: exit,
                currency: currency,
                onConfirm: onConfirm,
                onDismiss: onDismiss,
                onBackToGroups: onDismiss,
            ),
        )
    }
}

private struct GroupExitDialogs: ViewModifier {
    let exit: GroupExitState?
    let currency: GroupCurrency?
    let onConfirm: () -> Void
    let onDismiss: () -> Void
    let onBackToGroups: () -> Void

    private var dialog: (any GroupExitDialog)? {
        guard let exit, exit.deletedNotice == nil, !exit.isRunning else { return nil }
        return exit.dialog
    }

    func body(content: Content) -> some View {
        content
            .overlay {
                if exit?.isRunning == true {
                    ProgressView()
                        .controlSize(.large)
                        .padding(24)
                        .glassEffect(.regular, in: .rect(cornerRadius: 20))
                }
            }
            .alert(
                Text(verbatim: dialog.map(GroupExitCopy.title) ?? ""),
                isPresented: dialogShown,
                presenting: dialog,
            ) { dialog in
                buttons(dialog)
            } message: { dialog in
                Text(verbatim: GroupExitCopy.message(dialog, currency: currency))
            }
            .alert("Group deleted", isPresented: deletedShown, presenting: exit?.deletedNotice) { _ in
                Button("Back to Groups", action: onBackToGroups)
            } message: { notice in
                Text(verbatim: GroupExitCopy.deleted(notice))
            }
    }

    @ViewBuilder
    private func buttons(_ dialog: any GroupExitDialog) -> some View {
        switch dialog {
        case let blocked as GroupExitDialogLeaveBlocked:
            Button(blocked.canSettle ? LocalizedStringKey("Settle up") : LocalizedStringKey("See balances"), action: onConfirm)
            Button("Not now", role: .cancel, action: onDismiss)
        case is GroupExitDialogDeleteBlocked:
            Button("See balances", action: onConfirm)
            Button("Not now", role: .cancel, action: onDismiss)
        case is GroupExitDialogConfirmLeave:
            Button("Leave", role: .destructive, action: onConfirm)
            Button("Cancel", role: .cancel, action: onDismiss)
        default:
            Button("Delete for everyone", role: .destructive, action: onConfirm)
            Button("Cancel", role: .cancel, action: onDismiss)
        }
    }

    /// Dismissing the alert any other way than its buttons closes it in the ViewModel too; a button already did.
    private var dialogShown: Binding<Bool> {
        Binding(
            get: { dialog != nil },
            set: { shown in
                if !shown, dialog != nil { onDismiss() }
            },
        )
    }

    private var deletedShown: Binding<Bool> {
        Binding(
            get: { exit?.deletedNotice != nil },
            set: { shown in
                if !shown, exit?.deletedNotice != nil { onBackToGroups() }
            },
        )
    }
}
