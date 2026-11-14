import SwiftUI

/// Buttons side by side at equal widths while the widest one's label fits in its share on one line, else stacked,
/// each the whole width: a narrow column or a large text size never squeezes a capsule around a cut label (beta
/// feedback of 2026-11-14). Android's `FittingButtonsRow` decides the same way.
struct FittingButtonsLayout: Layout {
    var spacing: CGFloat = 10

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache _: inout ()) -> CGSize {
        guard !subviews.isEmpty else { return .zero }
        let ideals = subviews.map { $0.sizeThatFits(.unspecified) }
        let rowWidth = rowWidthNeeded(ideals)
        let width = proposal.width ?? rowWidth
        if rowWidth <= width {
            let itemWidth = itemWidth(in: width, count: subviews.count)
            let height = subviews.map { $0.sizeThatFits(ProposedViewSize(width: itemWidth, height: nil)).height }.max() ?? 0
            return CGSize(width: width, height: height)
        }
        let heights = subviews.map { $0.sizeThatFits(ProposedViewSize(width: width, height: nil)).height }
        return CGSize(width: width, height: heights.reduce(0, +) + spacing * CGFloat(subviews.count - 1))
    }

    func placeSubviews(in bounds: CGRect, proposal _: ProposedViewSize, subviews: Subviews, cache _: inout ()) {
        guard !subviews.isEmpty else { return }
        let ideals = subviews.map { $0.sizeThatFits(.unspecified) }
        if rowWidthNeeded(ideals) <= bounds.width {
            let itemWidth = itemWidth(in: bounds.width, count: subviews.count)
            var x = bounds.minX
            for subview in subviews {
                subview.place(
                    at: CGPoint(x: x, y: bounds.midY),
                    anchor: .leading,
                    proposal: ProposedViewSize(width: itemWidth, height: bounds.height),
                )
                x += itemWidth + spacing
            }
        } else {
            var y = bounds.minY
            for subview in subviews {
                let proposal = ProposedViewSize(width: bounds.width, height: nil)
                subview.place(at: CGPoint(x: bounds.minX, y: y), anchor: .topLeading, proposal: proposal)
                y += subview.sizeThatFits(proposal).height + spacing
            }
        }
    }

    /// The widest button times their count, so each equal share holds the longest label.
    private func rowWidthNeeded(_ ideals: [CGSize]) -> CGFloat {
        (ideals.map(\.width).max() ?? 0) * CGFloat(ideals.count) + spacing * CGFloat(ideals.count - 1)
    }

    private func itemWidth(in width: CGFloat, count: Int) -> CGFloat {
        (width - spacing * CGFloat(count - 1)) / CGFloat(count)
    }
}
