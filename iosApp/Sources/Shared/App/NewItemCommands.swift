#if os(macOS)
import AppKit
import Shared
import SwiftUI

/// What File › Split a Bill (⌘N) and New Group (⇧⌘N) do in the main window: the toolbar's buttons of the screen in
/// front, nil where it has none. `MainTabView` publishes it for the screen it knows is in front (`front`, the back
/// stack's last entry); a group's screen publishes its own button apart (`GroupSplitBillCommand`).
struct NewItemActions {
    /// The back stack's last entry, nil on Home: the group's screen whose "Split a bill" the menu may run.
    let front: AppRoute?
    /// Home's or Groups' "Split a bill", which asks for the group.
    let splitBill: (() -> Void)?
    /// Groups' "New group".
    let newGroup: (() -> Void)?
}

/// A group's screen's "Split a bill", for the route it draws, published only while that route is in front
/// (`frontRoute`), so a group's screen still under another one never takes ⌘N.
struct GroupSplitBillCommand {
    let host: AppRoute
    let run: () -> Void
}

extension EnvironmentValues {
    /// The main window's back stack's last entry, nil on Home: a pushed screen publishes what its menus run only while
    /// it is the one in front, never from under another screen it opened (two group screens, Activity under a group).
    @Entry var frontRoute: AppRoute?
}

extension FocusedValues {
    /// The main window's ⌘N and ⇧⌘N, nil while a sheet covers it.
    @Entry var newItemActions: NewItemActions?
    /// The "Split a bill" of the group's screen drawn in the main window.
    @Entry var groupSplitBill: GroupSplitBillCommand?
}

/// File › Split a Bill (⌘N) and New Group (⇧⌘N), in New Window's place (one window, `BoraRacharApp`): one command
/// for whichever screen is in front, as its toolbar's button, disabled where it has none (Balances, Contacts,
/// Activity, Search) or under a sheet.
struct NewItemCommands: Commands {
    /// Profile › Language, whose change draws the menu again: the menu bar has no `locale` of the windows to follow.
    let language: AppLanguage
    @FocusedValue(\.newItemActions) private var actions
    @FocusedValue(\.groupSplitBill) private var groupSplitBill

    var body: some Commands {
        CommandGroup(replacing: .newItem) {
            Button { run(splitBill) } label: { Text("Split a Bill", bundle: .appLanguage) }
                .keyboardShortcut("n", modifiers: .command)
                .disabled(splitBill == nil)
            Button { run(actions?.newGroup) } label: { Text("New Group", bundle: .appLanguage) }
                .keyboardShortcut("n", modifiers: [.command, .shift])
                .disabled(actions?.newGroup == nil)
        }
    }

    /// The group's own button while its screen is in front, else Home's or Groups'.
    private var splitBill: (() -> Void)? {
        guard let actions else { return nil }
        if let groupSplitBill, let front = actions.front, groupSplitBill.host == front { return groupSplitBill.run }
        return actions.splitBill
    }

    /// Never under an alert or a sheet a screen presents itself, which `isWindowUncovered` can't see.
    private func run(_ action: (() -> Void)?) {
        guard !NSApplication.hasSheetOpen else { return }
        action?()
    }
}
#endif
