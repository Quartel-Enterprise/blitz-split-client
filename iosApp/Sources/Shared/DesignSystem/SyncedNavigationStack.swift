import SwiftUI

/// A NavigationStack drawing a tab's slice of the shared back stack. It holds the path it shows in its own state and
/// follows the shared one both ways: the Mac's `TabView` never pushed what a tab other than the first one shown got
/// through a computed binding (a group's row in Groups, a group from Balances; Pierre's beta feedback), although the
/// binding already held it.
struct SyncedNavigationStack<Root: View>: View {
    @Binding var path: [AppRoute]
    @ViewBuilder let root: () -> Root

    @State private var shown: [AppRoute] = []

    var body: some View {
        NavigationStack(path: $shown) {
            root()
        }
        .onChange(of: path, initial: true) { _, newPath in
            if shown != newPath { shown = newPath }
        }
        .onChange(of: shown) { _, newShown in
            // A back gesture or the back button: the shared back stack catches up
            if newShown != path { path = newShown }
        }
    }
}
