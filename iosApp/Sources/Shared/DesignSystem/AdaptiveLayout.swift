import SwiftUI

/// The canvas draws every screen upright, lying down, on iPad and in a Mac window. Lying down (or in
/// a wide Mac window) a screen splits into two columns; upright it is one centred column.
struct ScreenLayout: Equatable {
    var size: CGSize = .zero

    var usesTwoColumns: Bool {
        size.width > size.height && size.width >= 640
    }

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
