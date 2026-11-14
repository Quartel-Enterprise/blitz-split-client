import SwiftUI

/// A row's text and what trails it (an amount, a button) side by side while the text keeps room to read, the trailing
/// part under the text once it doesn't. In a narrow column (an iPhone lying down, the group beside the list) the
/// amount used to squeeze the member's name and "This iPhone" (beta feedback of 2026-11-14). Side by side the text
/// takes the width the trailing part leaves, the two centered on each other; stacked both start at the leading edge.
/// The text needs `minMainWidth`, or less when all of it fits in less. Takes exactly two subviews: the text, then
/// the trailing part.
struct TrailingOrBelowLayout: Layout {
    var minMainWidth: CGFloat = 160
    var spacing: CGFloat = 12
    var belowSpacing: CGFloat = 4

    /// The two subviews' ideal sizes, measured once per layout pass rather than on every proposal.
    struct Cache {
        var ideals: [CGSize]
    }

    func makeCache(subviews: Subviews) -> Cache {
        Cache(ideals: subviews.map { $0.sizeThatFits(.unspecified) })
    }

    func updateCache(_ cache: inout Cache, subviews: Subviews) {
        cache = makeCache(subviews: subviews)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) -> CGSize {
        guard subviews.count == 2 else {
            return subviews.first?.sizeThatFits(proposal) ?? .zero
        }
        let arrangement = arrange(width: proposal.width, subviews: subviews, ideals: cache.ideals)
        return arrangement.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) {
        guard subviews.count == 2 else {
            subviews.first?.place(at: bounds.origin, proposal: ProposedViewSize(bounds.size))
            return
        }
        let arrangement = arrange(width: bounds.width, subviews: subviews, ideals: cache.ideals)
        let main = subviews[0]
        let trailing = subviews[1]
        if arrangement.isSideBySide {
            main.place(
                at: CGPoint(x: bounds.minX, y: bounds.midY),
                anchor: .leading,
                proposal: ProposedViewSize(width: arrangement.mainWidth, height: nil),
            )
            trailing.place(
                at: CGPoint(x: bounds.maxX, y: bounds.midY),
                anchor: .trailing,
                proposal: ProposedViewSize(width: arrangement.trailingSize.width, height: nil),
            )
        } else {
            main.place(at: bounds.origin, proposal: ProposedViewSize(width: bounds.width, height: nil))
            trailing.place(
                at: CGPoint(x: bounds.minX, y: bounds.minY + arrangement.mainHeight + belowSpacing),
                proposal: ProposedViewSize(width: bounds.width, height: nil),
            )
        }
    }

    private struct Arrangement {
        let isSideBySide: Bool
        let mainWidth: CGFloat
        let mainHeight: CGFloat
        let trailingSize: CGSize
        let size: CGSize
    }

    private func arrange(width: CGFloat?, subviews: Subviews, ideals: [CGSize]) -> Arrangement {
        let main = subviews[0]
        let trailing = subviews[1]
        let mainIdeal = ideals[0]
        let trailingIdeal = ideals[1]
        guard let width, width.isFinite else {
            let height = max(mainIdeal.height, trailingIdeal.height)
            return Arrangement(
                isSideBySide: true,
                mainWidth: mainIdeal.width,
                mainHeight: mainIdeal.height,
                trailingSize: trailingIdeal,
                size: CGSize(width: mainIdeal.width + spacing + trailingIdeal.width, height: height),
            )
        }
        let room = width - trailingIdeal.width - spacing
        if trailingIdeal.width == 0 || room >= min(mainIdeal.width, minMainWidth) {
            let mainWidth = max(0, trailingIdeal.width == 0 ? width : room)
            let mainSize = main.sizeThatFits(ProposedViewSize(width: mainWidth, height: nil))
            return Arrangement(
                isSideBySide: true,
                mainWidth: mainWidth,
                mainHeight: mainSize.height,
                trailingSize: trailingIdeal,
                size: CGSize(width: width, height: max(mainSize.height, trailingIdeal.height)),
            )
        }
        let mainSize = main.sizeThatFits(ProposedViewSize(width: width, height: nil))
        let trailingSize = trailing.sizeThatFits(ProposedViewSize(width: width, height: nil))
        return Arrangement(
            isSideBySide: false,
            mainWidth: width,
            mainHeight: mainSize.height,
            trailingSize: trailingSize,
            size: CGSize(width: width, height: mainSize.height + belowSpacing + trailingSize.height),
        )
    }
}
