import SwiftUI

/// One screen on an iPhone Duo open upright, as a notebook (iOS 27.1), where the boards draw their own version (2.2,
/// 3.1, 6.2, 6.5, 6.6, 7.1, 7.2): `top` above the fold and `bottom` below it,
/// each scrolling on its own, the gap covering the fold whole (a Form or a List in each half scrolls itself:
/// `scrollsHalves` false). Everywhere else, the Duo lying down or closed included, the screen's own `single` column.
/// What was typed lives in the ViewModel, so folding the device never loses it.
struct FoldHalves<Top: View, Bottom: View, Single: View>: View {
    /// The top half's content centered in its half while it fits (Convidar's QR card); else from the top.
    private let centersTop: Bool
    private let scrollsHalves: Bool
    private let top: Top
    private let bottom: Bottom
    private let single: Single

    @State private var division: FoldDivision?

    init(
        centersTop: Bool = false,
        scrollsHalves: Bool = true,
        @ViewBuilder top: () -> Top,
        @ViewBuilder bottom: () -> Bottom,
        @ViewBuilder single: () -> Single,
    ) {
        self.centersTop = centersTop
        self.scrollsHalves = scrollsHalves
        self.top = top()
        self.bottom = bottom()
        self.single = single()
    }

    var body: some View {
        Group {
            if let fold = division?.horizontalFrame {
                VStack(spacing: 0) {
                    half(top)
                        .defaultScrollAnchor(centersTop ? .center : .top, for: .alignment)
                        .frame(height: max(0, fold.minY))
                    // The forms' background, never the sheet's white, over the fold
                    Palette.groupedBackground
                        .frame(height: fold.height)
                    half(bottom)
                        .frame(maxHeight: .infinity)
                }
            } else {
                single
            }
        }
        // The whole screen reads the fold, so its frame and the halves share their coordinates
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .readingFoldDivision($division)
    }

    @ViewBuilder
    private func half(_ content: some View) -> some View {
        if scrollsHalves {
            ScrollView {
                content
            }
            .scrollBounceBehavior(.basedOnSize)
        } else {
            content
        }
    }
}
