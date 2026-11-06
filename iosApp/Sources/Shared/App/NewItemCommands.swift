#if os(macOS)
import AppKit
import Shared
import SwiftUI

/// What File › Split a Bill (⌘N) and New Group (⇧⌘N) run in the main window: the toolbar's buttons of the screen in
/// front, nil where it has none. Each screen with such a button publishes its own (`newItemCommands`).
struct NewItemActions {
    /// "Split a bill": Home's and Groups' ask for the group, a group's screen's is a bill in that group.
    let splitBill: (() -> Void)?
    /// Groups' "New group".
    let newGroup: (() -> Void)?
}

extension EnvironmentValues {
    /// The main window's back stack's last entry, nil on Home: a screen publishes what its menus run only while it is
    /// the one in front, never from a tab behind another nor from under a screen it opened.
    @Entry var frontRoute: AppRoute?
}

extension FocusedValues {
    /// The main window's ⌘N and ⇧⌘N, published by the screen in front.
    @Entry var newItemActions: NewItemActions?
}

extension View {
    /// Publishes this screen's ⌘N and ⇧⌘N while it is in front and no sheet covers the window. Elsewhere it publishes
    /// nothing (a nil value leaves the key to the screen that is in front), so the one screen in front decides.
    func newItemCommands(isInFront: Bool, splitBill: (() -> Void)?, newGroup: (() -> Void)? = nil) -> some View {
        modifier(NewItemPublisher(isInFront: isInFront, splitBill: splitBill, newGroup: newGroup))
    }
}

private struct NewItemPublisher: ViewModifier {
    let isInFront: Bool
    let splitBill: (() -> Void)?
    let newGroup: (() -> Void)?

    @Environment(\.isWindowUncovered) private var isWindowUncovered

    func body(content: Content) -> some View {
        content.focusedSceneValue(
            \.newItemActions,
            isInFront && isWindowUncovered ? NewItemActions(splitBill: splitBill, newGroup: newGroup) : nil,
        )
    }
}

/// File › Split a Bill (⌘N) and New Group (⇧⌘N), in New Window's place (one window, `BoraRacharApp`): one command
/// for whichever screen is in front, as its toolbar's button, disabled where it has none (Balances, Contacts,
/// Activity, Search) or under a sheet.
struct NewItemCommands: Commands {
    /// Profile › Language, whose change draws the menu again: the menu bar has no `locale` of the windows to follow.
    let language: AppLanguage
    @FocusedValue(\.newItemActions) private var actions

    var body: some Commands {
        CommandGroup(replacing: .newItem) {
            Button { run(actions?.splitBill) } label: { Text("Split a Bill", bundle: .appLanguage) }
                .keyboardShortcut("n", modifiers: .command)
                .disabled(actions?.splitBill == nil)
            Button { run(actions?.newGroup) } label: { Text("New Group", bundle: .appLanguage) }
                .keyboardShortcut("n", modifiers: [.command, .shift])
                .disabled(actions?.newGroup == nil)
        }
    }

    /// Never under an alert or a sheet a screen presents itself (the recovery code's prompt included), which
    /// `isWindowUncovered` can't see.
    private func run(_ action: (() -> Void)?) {
        guard !NSApplication.hasSheetOpen else { return }
        action?()
    }
}
#endif
