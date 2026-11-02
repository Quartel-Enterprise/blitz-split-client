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
    /// The screen has halves to show now (Statistics with bills); else `single` even on the fold, in the same place
    /// as without one, so switching keeps its scroll.
    private let splits: Bool
    private let top: Top
    private let bottom: Bottom
    private let single: Single

    /// The fold over the whole window, the halves' first guess until this view reads its own.
    @Environment(\.windowFold) private var windowFold
    @State private var division: FoldDivision?
    /// This view read its own fold (or that none crosses it), which replaces the window's from then on.
    @State private var hasReadDivision = false

    init(
        centersTop: Bool = false,
        scrollsHalves: Bool = true,
        splits: Bool = true,
        @ViewBuilder top: () -> Top,
        @ViewBuilder bottom: () -> Bottom,
        @ViewBuilder single: () -> Single,
    ) {
        self.centersTop = centersTop
        self.scrollsHalves = scrollsHalves
        self.splits = splits
        self.top = top()
        self.bottom = bottom()
        self.single = single()
    }

    var body: some View {
        Group {
            if splits, let fold {
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
        .readingFoldDivision(
            Binding(
                get: { division },
                set: { newDivision in
                    division = newDivision
                    hasReadDivision = true
                },
            ),
        )
    }

    /// The fold across this view. Before its first reading, the window's, so a screen opening on the fold draws its
    /// halves from the first frame: drawing `single` first and swapping would rebuild what it holds, and drop the
    /// focus a field asked for on appearing (Split a bill's amount).
    private var fold: CGRect? {
        hasReadDivision ? division?.horizontalFrame : windowFold?.horizontalFrame
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
