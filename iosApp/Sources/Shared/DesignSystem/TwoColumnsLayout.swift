import SwiftUI

/// Two columns of one screen, as the canvas draws them lying down and with an iPad's room: `first` at `firstWidth`
/// (half of what is left when nil) and `second` beside it, or `second` at `secondWidth` with `first` taking the rest
/// (Convidar's QR card), `spacing` apart, in a container of at most `maxWidth` (920 pt), top-aligned unless
/// `alignment` says otherwise. On an iPhone Duo (iOS 27.1) whose fold runs down the screen, each column takes its side
/// of the fold and the gap covers it whole, centered on it (the boards' 412 | 412 split on x = 460); everywhere else
/// the size alone decides. The screen picks two columns itself (`ScreenLayout.usesTwoColumns`), and each column scrolls
/// on its own if it needs to.
struct TwoColumnsLayout<First: View, Second: View>: View {
    private let firstWidth: CGFloat?
    private let secondWidth: CGFloat?
    private let spacing: CGFloat
    private let maxWidth: CGFloat
    private let alignment: VerticalAlignment
    private let first: First
    private let second: Second

    @State private var division: FoldDivision?

    init(
        firstWidth: CGFloat? = nil,
        secondWidth: CGFloat? = nil,
        spacing: CGFloat = 48,
        maxWidth: CGFloat = ScreenLayout.twoColumnsMaxWidth,
        alignment: VerticalAlignment = .top,
        @ViewBuilder first: () -> First,
        @ViewBuilder second: () -> Second,
    ) {
        self.firstWidth = firstWidth
        self.secondWidth = secondWidth
        self.spacing = spacing
        self.maxWidth = maxWidth
        self.alignment = alignment
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
                HStack(alignment: alignment, spacing: 0) {
                    first.frame(width: gapStart, alignment: .top)
                    Color.clear.frame(width: gapEnd - gapStart, height: 0)
                    second.frame(maxWidth: .infinity, alignment: .top)
                }
            } else {
                HStack(alignment: alignment, spacing: spacing) {
                    if let firstWidth {
                        first.frame(width: firstWidth, alignment: .top)
                    } else {
                        first.frame(maxWidth: .infinity, alignment: .top)
                    }
                    if let secondWidth {
                        second.frame(width: secondWidth, alignment: .top)
                    } else {
                        second.frame(maxWidth: .infinity, alignment: .top)
                    }
                }
                .frame(maxWidth: maxWidth)
            }
        }
        // The full width reads the fold, so its frame and the columns share their coordinates
        .frame(maxWidth: .infinity)
        .readingFoldDivision($division)
    }
}
