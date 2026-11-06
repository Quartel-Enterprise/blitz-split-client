import SwiftUI

/// A NavigationStack drawing a tab's slice of the shared back stack. On the Mac it holds the path it shows in its own
/// state and follows the shared one both ways: the Mac's `TabView` never pushed what a tab other than the first one
/// shown got through a computed binding (a group's row in Groups, a group from Balances; Pierre's beta feedback),
/// although the binding already held it. iOS draws the binding as it is, which works there.
struct SyncedNavigationStack<Root: View>: View {
    @Binding var path: [AppRoute]
    @ViewBuilder let root: () -> Root

    #if os(macOS)
    /// Starts on the bound path, so a tab first shown with screens above it draws them at once rather than pushing.
    @State private var shown: [AppRoute]
    #endif

    init(path: Binding<[AppRoute]>, @ViewBuilder root: @escaping () -> Root) {
        _path = path
        self.root = root
        #if os(macOS)
        _shown = State(initialValue: path.wrappedValue)
        #endif
    }

    var body: some View {
        #if os(macOS)
        NavigationStack(path: $shown) {
            root()
        }
        .onChange(of: path, initial: true) { _, newPath in
            if shown != newPath { shown = newPath }
        }
        .onChange(of: shown) { _, newShown in
            // A back gesture or the back button: the shared back stack catches up
            guard newShown != path else { return }
            path = newShown
            // The shared stack answers through its watcher, queued on the main queue before this; a pop it refused
            // (or undid at once) leaves the bound path as it was, which no change would bring back otherwise
            DispatchQueue.main.async {
                if shown != path { shown = path }
            }
        }
        #else
        NavigationStack(path: $path) {
            root()
        }
        #endif
    }
}
