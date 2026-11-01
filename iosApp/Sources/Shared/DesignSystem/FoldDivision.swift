import SwiftUI

/// The fold of an iPhone Duo where it crosses a view, as the iOS 27.1 SDK reports it: a division reserved region,
/// in that view's own coordinates (mirrored in right-to-left, as a SwiftUI `Layout` places), margins included. The
/// fold counts open flat too (the boards split there, "plano ou como livro"), so inactive regions are read as well:
/// `isActive` is the device partially open, a book when the fold runs down (`isVertical`) and a notebook when it
/// runs across. Nil below iOS 27.1, on the Mac and on any device without a fold, where the layouts fall back on the
/// window's size (`ScreenLayout`). Apple steers layout to these regions rather than to the hinge's angle
/// (`onHingeChange`), which is for interactions, so that one isn't read here.
nonisolated struct FoldDivision: Equatable {
    var frame: CGRect
    var isActive: Bool

    var isVertical: Bool {
        frame.height > frame.width
    }
}

extension View {
    /// Keeps `division` on the fold that crosses this view (see `FoldDivision`), read from a background the size of
    /// this view, so its frame is in the same coordinates as what this view lays out.
    func readingFoldDivision(_ division: Binding<FoldDivision?>) -> some View {
        #if os(iOS)
        background {
            GeometryReader { proxy in
                Color.clear
                    .onChange(of: foldDivision(in: proxy), initial: true) { _, newDivision in
                        division.wrappedValue = newDivision
                    }
            }
        }
        #else
        self
        #endif
    }
}

#if os(iOS)
/// The iPhone Duo's single fold, the first division over the view; the SDK has the API from iOS 27.1 only.
private func foldDivision(in proxy: GeometryProxy) -> FoldDivision? {
    guard #available(iOS 27.1, *) else { return nil }
    guard let region = proxy.reservedRegions(kind: .division, options: .includeInactive).first else { return nil }
    return FoldDivision(frame: region.frame, isActive: region.isActive)
}
#endif
