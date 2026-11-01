import Charts
import Shared
import SwiftUI

/// "Over time" with Swift Charts in the accent color: bars, or a line over a light area with a point on each value
/// up to 12 (only the most expensive one past that), scaled by the most expensive bar. The labels the shared code
/// marks go under it, and VoiceOver reads the whole series.
struct StatsChart: View {
    let state: GroupStatsUiState

    private struct Point: Identifiable {
        let id: Int
        let key: String
        let amount: Double
        let showsDot: Bool
    }

    private var points: [Point] {
        state.bars.enumerated().map { index, bar in
            Point(id: index, key: "\(index)", amount: Double(bar.amountMinor), showsDot: bar.showsDot)
        }
    }

    var body: some View {
        let rows = points
        let largest = max(rows.map(\.amount).max() ?? 1, 1)
        VStack(spacing: 6) {
            if state.chartType == .line {
                styled(
                    Chart(rows) { point in
                        AreaMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                            .foregroundStyle(Palette.accent.opacity(0.14))
                        LineMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                            .foregroundStyle(Palette.accent)
                            .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))
                        if point.showsDot {
                            PointMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                                .foregroundStyle(Palette.accent)
                                .symbolSize(50)
                        }
                    },
                    largest: largest,
                )
            } else {
                styled(
                    Chart(rows) { point in
                        // A day without bills keeps a short grey stub, as the canvas draws it
                        BarMark(
                            x: .value("Period", point.key),
                            y: .value("Amount", point.amount > 0 ? max(point.amount, largest * 0.03) : largest * 0.03),
                            width: .ratio(rows.count > 20 ? 0.8 : 0.7),
                        )
                        .foregroundStyle(point.amount > 0 ? Palette.accent : Color.secondary.opacity(0.2))
                        .cornerRadius(rows.count > 20 ? 2 : 6)
                    },
                    largest: largest,
                )
            }
            labels
        }
    }

    /// No axes nor legend, as the canvas draws it, and the whole series for VoiceOver.
    private func styled(_ chart: some View, largest: Double) -> some View {
        chart
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .chartYScale(domain: 0...largest)
            .chartLegend(.hidden)
            .frame(height: 128)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: GroupStatsCopy.chartDescription(state)))
    }

    /// Each label centered under its bar, so a few labels of many bars still line up with them.
    private var labels: some View {
        GeometryReader { geometry in
            let count = max(state.bars.count, 1)
            let slot = geometry.size.width / CGFloat(count)
            ZStack(alignment: .topLeading) {
                ForEach(Array(state.bars.enumerated()), id: \.offset) { index, bar in
                    if bar.showsLabel {
                        let isLast = !state.hasSparseLabels && index == state.bars.count - 1
                        Text(verbatim: GroupStatsCopy.barLabel(bar, grain: state.grain, isSparse: state.hasSparseLabels))
                            .font(.caption)
                            .fontWeight(isLast ? .semibold : .regular)
                            .foregroundStyle(isLast ? .primary : .secondary)
                            .lineLimit(1)
                            .fixedSize()
                            .position(x: (CGFloat(index) + 0.5) * slot, y: 8)
                    }
                }
            }
        }
        .frame(height: 16)
        .accessibilityHidden(true)
    }
}
