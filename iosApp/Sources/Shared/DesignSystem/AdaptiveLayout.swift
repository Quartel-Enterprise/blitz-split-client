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

    /// The iPhone Duo closed (about 452×648 pt): upright, wider than any iPhone (440 pt at most) yet short of an
    /// iPad's room, and shorter than any iPhone upright, where the boards drop what doesn't fit (Welcome's benefits).
    var isShortColumn: Bool {
        size.width < size.height && size.width > 440 && !isLarge && size.height < 700
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

extension View {
    /// The root's measure of the whole window: hands it to every view below as `windowLayout` and reports each
    /// measure to `onMeasure`. The size lives in this modifier's own state, so a live resize (Stage Manager, Split
    /// View) redraws only the views that read `windowLayout`, never the root's body.
    func measuringWindow(onMeasure: @escaping (ScreenLayout) -> Void) -> some View {
        modifier(WindowMeasuring(onMeasure: onMeasure))
    }
}

private struct WindowMeasuring: ViewModifier {
    let onMeasure: (ScreenLayout) -> Void
    @State private var window = ScreenLayout()

    func body(content: Content) -> some View {
        content
            .measuringScreenLayout($window)
            .environment(\.windowLayout, window)
            // The first measure moves it off `.zero`, so the start counts as a change too
            .onChange(of: window) { _, newWindow in
                onMeasure(newWindow)
            }
    }
}

extension EnvironmentValues {
    /// The whole window, measured once by the root, so a screen inside a sheet still knows what it opened over: an
    /// iPad's room opens a key and a bill as sheets (`AppRoute.presentsAsSheet(isLargeWindow:)`), an iPhone's pushes
    /// them.
    @Entry var windowLayout = ScreenLayout()
}
