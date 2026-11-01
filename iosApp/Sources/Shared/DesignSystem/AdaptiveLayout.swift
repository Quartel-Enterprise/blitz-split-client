import SwiftUI

/// The canvas draws every screen upright, lying down, on iPad and in a Mac window. Lying down (or in
/// a wide Mac window) a screen splits into two columns; upright it is one centred column. Every rule is the
/// window's size, never the device's idiom, so an iPhone Duo open (920×648) gets what the boards draw for that
/// size: an iPad's layouts lying down (648 pt on its short side) and upright (648×920). See
/// docs/architecture/responsive-layouts.md.
struct ScreenLayout: Equatable {
    var size: CGSize = .zero

    var usesTwoColumns: Bool {
        size.width > size.height && size.width >= 640
    }

    /// An iPad's room, by size: at least 600 pt on the short side (an iPad, an iPhone Duo open, a wide Split View).
    var isLarge: Bool {
        min(size.width, size.height) >= 600
    }

    /// The widest a single column grows (520 on iPad, the canvas rule).
    static let singleColumnMaxWidth: CGFloat = 520

    /// The two-column container never stretches edge to edge.
    static let twoColumnsMaxWidth: CGFloat = 920
}

extension View {
    /// Measures the whole window, keyboard included, so opening the keyboard never flips the layout
    /// and rebuilds the focused field.
    func measuringScreenLayout(_ layout: Binding<ScreenLayout>) -> some View {
        background {
            Color.clear
                .ignoresSafeArea()
                .onGeometryChange(for: CGSize.self, of: \.size) { size in
                    layout.wrappedValue = ScreenLayout(size: size)
                }
        }
    }
}

extension EnvironmentValues {
    /// The whole window, measured once by the root, so a screen inside a sheet still knows what it opened over: an
    /// iPad's room opens a key and a bill as sheets (`AppRoute.presentsAsSheet(in:)`), an iPhone's pushes them.
    @Entry var windowLayout = ScreenLayout()
}
