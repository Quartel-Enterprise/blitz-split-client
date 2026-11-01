import SwiftUI

/// Two columns of one screen, as the canvas draws them lying down and with an iPad's room: `first` at `firstWidth`
/// (half of what is left when nil) and `second` beside it, `spacing` apart, in a container of at most 920 pt, both
/// top-aligned. On an iPhone Duo (iOS 27.1) whose fold runs down the screen, each column takes its side of the fold
/// and the gap covers it whole, centered on it (the boards' 412 | 412 split on x = 460); everywhere else the size
/// alone decides. The screen picks two columns itself (`ScreenLayout.usesTwoColumns`), and each column scrolls
/// on its own if it needs to.
struct TwoColumnsLayout<First: View, Second: View>: View {
    private let firstWidth: CGFloat?
    private let spacing: CGFloat
    private let first: First
    private let second: Second

    @State private var division: FoldDivision?

    init(
        firstWidth: CGFloat? = nil,
        spacing: CGFloat = 48,
        @ViewBuilder first: () -> First,
        @ViewBuilder second: () -> Second,
    ) {
        self.firstWidth = firstWidth
        self.spacing = spacing
        self.first = first()
        self.second = second()
    }

    private var verticalFold: CGRect? {
        guard let division, division.isVertical else { return nil }
        return division.frame
    }

    var body: some View {
        Group {
            if let fold = verticalFold {
                let gapStart = max(0, min(fold.midX - spacing / 2, fold.minX))
                let gapEnd = max(gapStart, fold.midX + spacing / 2, fold.maxX)
                HStack(alignment: .top, spacing: 0) {
                    first.frame(width: gapStart, alignment: .top)
                    Color.clear.frame(width: gapEnd - gapStart, height: 0)
                    second.frame(maxWidth: .infinity, alignment: .top)
                }
            } else {
                HStack(alignment: .top, spacing: spacing) {
                    if let firstWidth {
                        first.frame(width: firstWidth, alignment: .top)
                    } else {
                        first.frame(maxWidth: .infinity, alignment: .top)
                    }
                    second.frame(maxWidth: .infinity, alignment: .top)
                }
                .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
            }
        }
        // The full width reads the fold, so its frame and the columns share their coordinates
        .frame(maxWidth: .infinity)
        .readingFoldDivision($division)
    }
}
