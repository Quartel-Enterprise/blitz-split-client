import Accessibility
import Charts
import Shared
import SwiftUI

/// "Over time" with Swift Charts in the accent color: bars, or a line over a light area with a point on each value
/// up to 12 (only the most expensive one past that), scaled by the top value line. Dashed value lines with their
/// short amount on the trailing side and the dashed average sit behind it, the labels the shared code marks go under
/// it, and VoiceOver reads the whole series and plays it as an Audio Graph. A bar or point picked by a tap, a drag,
/// the pointer or the Mac's arrow keys shows its period, amount and bills over it; a tap keeps it until a second tap,
/// a click keeps what hovering picked. The bars grow from the base, and the line draws in from the leading side, when
/// the grain, the period or the chart type changes.
struct StatsChart: View {
    let state: GroupStatsUiState

    @State private var selected: Int?
    @State private var isHovering = false
    /// 0 to 1 as the series comes in: the bars grow from the base and the line draws in from the leading side.
    @State private var growth: Double = 0

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

    /// What resets the pick and plays the change: the grain, the period, the chart type and how many bars.
    private var seriesKey: String {
        "\(state.grain.name)-\(state.periodStartMillis)-\(state.periodEndMillis)-\(state.chartType.name)-\(state.bars.count)"
    }

    var body: some View {
        let rows = points
        let top = Double(state.chartTopMinor)
        let grown = growth
        let average = state.chartAverageMinor.map { Double(truncating: $0) }
        Group {
            if state.chartType == .line {
                styled(
                    Chart {
                        ForEach(rows) { point in
                            AreaMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                                .foregroundStyle(Palette.accent.opacity(0.14))
                            LineMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                                .foregroundStyle(Palette.accent)
                                .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))
                        }
                        if let selected, rows.indices.contains(selected) {
                            RuleMark(x: .value("Period", rows[selected].key))
                                .foregroundStyle(Color.secondary.opacity(0.5))
                                .lineStyle(StrokeStyle(lineWidth: 1))
                        }
                        averageMark(average)
                        ForEach(rows) { point in
                            if point.showsDot || point.id == selected {
                                PointMark(x: .value("Period", point.key), y: .value("Amount", point.amount))
                                    .foregroundStyle(Palette.accent)
                                    .symbolSize(point.id == selected ? 110 : 50)
                                    .annotation(position: .top, spacing: 6, overflowResolution: calloutOverflow) {
                                        if point.id == selected { callout(point.id) }
                                    }
                            }
                        }
                    },
                    top: top,
                )
                // The line draws in from the leading side; the mask spills past the plot so callouts and dots fit
                .chartPlotStyle { plot in
                    plot.mask {
                        GeometryReader { geometry in
                            Rectangle()
                                .frame(width: (geometry.size.width + 400) * grown, height: geometry.size.height + 400)
                                .offset(x: -200, y: -200)
                        }
                    }
                }
            } else {
                styled(
                    Chart {
                        ForEach(rows) { point in
                            // A day without bills keeps a short grey stub, as the canvas draws it
                            BarMark(
                                x: .value("Period", point.key),
                                y: .value("Amount", point.amount > 0 ? max(point.amount * grown, top * 0.03) : top * 0.03),
                                width: .ratio(rows.count > 20 ? 0.8 : 0.7),
                            )
                            .foregroundStyle(point.amount > 0 ? Palette.accent : Color.secondary.opacity(0.2))
                            .opacity(selected == nil || selected == point.id ? 1 : 0.35)
                            .cornerRadius(rows.count > 20 ? 2 : 6)
                            .annotation(position: .top, spacing: 6, overflowResolution: calloutOverflow) {
                                if point.id == selected { callout(point.id) }
                            }
                        }
                        averageMark(average)
                    },
                    top: top,
                )
            }
        }
        .onChange(of: seriesKey) { selected = nil }
        .task(id: seriesKey) {
            growth = 0
            try? await Task.sleep(for: .milliseconds(20))
            withAnimation(.easeOut(duration: 0.5)) { growth = 1 }
        }
        .sensoryFeedback(.selection, trigger: selected) { _, new in new != nil && !isHovering }
    }

    private var calloutOverflow: AnnotationOverflowResolution {
        AnnotationOverflowResolution(x: .fit(to: .chart), y: .disabled)
    }

    @ChartContentBuilder
    private func averageMark(_ average: Double?) -> some ChartContent {
        if let average {
            RuleMark(y: .value("Average", average))
                .foregroundStyle(Color.secondary)
                .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
        }
    }

    /// The picked bar's "Oct 28", "R$ 1,458" and "3 expenses", on glass over it.
    private func callout(_ index: Int) -> some View {
        let bar = state.bars[index]
        return VStack(alignment: .leading, spacing: 1) {
            Text(verbatim: GroupStatsCopy.barName(bar, grain: state.grain))
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(verbatim: state.currency.format(bar.amountMinor))
                .font(.subheadline.weight(.semibold))
                .monospacedDigit()
            Text(verbatim: bar.expenseCount == 0
                ? String(
                    localized: "No expenses",
                    bundle: .appLanguage,
                    comment: "The group statistics: a picked bar of the chart without bills",
                )
                : GroupStatsCopy.expenseCount(bar.expenseCount))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .glassEffect(.regular, in: .rect(cornerRadius: 12))
    }

    /// The value lines with their short amounts on the trailing side, the labels the shared code marks under the bars,
    /// the pick by touch, pointer and keys, and the whole series for VoiceOver.
    private func styled(_ chart: some View, top: Double) -> some View {
        let ticks = state.chartTicksMinor.map { Double(truncating: $0) }
        let labeled = state.bars.enumerated().filter { $0.element.showsLabel }.map { "\($0.offset)" }
        return chart
            .chartYScale(domain: 0...top)
            .chartYAxis {
                AxisMarks(position: .trailing, values: ticks) { value in
                    let amount = value.as(Double.self) ?? 0
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: amount == 0 ? [] : [2, 3]))
                    if amount > 0 {
                        AxisValueLabel {
                            Text(verbatim: GroupStatsCopy.compactAmount(Int64(amount), currency: state.currency))
                                .font(.caption2)
                        }
                    }
                }
            }
            .chartXAxis {
                AxisMarks(values: labeled) { value in
                    let index = Int(value.as(String.self) ?? "") ?? 0
                    let isLast = !state.hasSparseLabels && index == state.bars.count - 1
                    AxisValueLabel(collisionResolution: .disabled) {
                        Text(verbatim: GroupStatsCopy.barLabel(state.bars[index], grain: state.grain, isSparse: state.hasSparseLabels))
                            .font(.caption)
                            .fontWeight(isLast ? .semibold : .regular)
                            .foregroundStyle(isLast ? .primary : .secondary)
                    }
                }
            }
            .chartLegend(.hidden)
            .chartOverlay { proxy in
                GeometryReader { geometry in
                    Rectangle()
                        .fill(.clear)
                        .contentShape(Rectangle())
                        .onTapGesture { location in
                            let index = index(at: location, proxy: proxy, geometry: geometry)
                            // A click keeps what hovering picked; a tap of a finger picks or lets go
                            selected = index == selected && !isHovering ? nil : index
                        }
                        .simultaneousGesture(
                            DragGesture(minimumDistance: 12)
                                .onChanged { drag in
                                    // A mostly vertical drag is the page scrolling, not a pick
                                    guard abs(drag.translation.width) > abs(drag.translation.height) else { return }
                                    selected = index(at: drag.location, proxy: proxy, geometry: geometry)
                                },
                        )
                        .onContinuousHover { phase in
                            switch phase {
                            case let .active(location):
                                isHovering = true
                                selected = index(at: location, proxy: proxy, geometry: geometry)
                            case .ended:
                                isHovering = false
                                selected = nil
                            }
                        }
                }
            }
            .frame(height: 150)
            // The arrow keys of the Mac move the pick; on iPhone and iPad focus would only get in the way of taps
            .focusable(Self.takesKeys)
            .focusEffectDisabled()
            .onKeyPress(.rightArrow) { move(by: 1) }
            .onKeyPress(.leftArrow) { move(by: -1) }
            .onKeyPress(.escape) {
                selected = nil
                return .handled
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: GroupStatsCopy.chartDescription(state)))
            .accessibilityChartDescriptor(StatsAudioGraph(state: state))
    }

    #if os(macOS)
        private static let takesKeys = true
    #else
        private static let takesKeys = false
    #endif

    private func move(by step: Int) -> KeyPress.Result {
        guard !state.bars.isEmpty else { return .ignored }
        let start = selected ?? (step > 0 ? -1 : state.bars.count)
        selected = min(max(start + step, 0), state.bars.count - 1)
        return .handled
    }

    private func index(at location: CGPoint, proxy: ChartProxy, geometry: GeometryProxy) -> Int? {
        guard let frame = proxy.plotFrame, !state.bars.isEmpty else { return nil }
        let plot = geometry[frame]
        let x = min(max(location.x - plot.origin.x, 0), plot.width - 1)
        let index = Int(x / (plot.width / CGFloat(state.bars.count)))
        return min(max(index, 0), state.bars.count - 1)
    }
}

/// The series as an Audio Graph for VoiceOver: each bar's name on the x axis, its amount on the y axis.
private struct StatsAudioGraph: AXChartDescriptorRepresentable {
    let state: GroupStatsUiState

    func makeChartDescriptor() -> AXChartDescriptor {
        let names = state.bars.map { GroupStatsCopy.barName($0, grain: state.grain) }
        let top = Double(state.chartTopMinor)
        let currency = state.currency
        let xAxis = AXCategoricalDataAxisDescriptor(title: String(
            localized: "Period",
            bundle: .appLanguage,
        ), categoryOrder: names)
        let yAxis = AXNumericDataAxisDescriptor(
            title: String(localized: "Amount", bundle: .appLanguage),
            range: 0...top,
            gridlinePositions: state.chartTicksMinor.map { Double(truncating: $0) },
        ) { value in currency.format(Int64(value)) }
        let series = AXDataSeriesDescriptor(
            name: String(localized: "Over time", bundle: .appLanguage),
            isContinuous: state.chartType == .line,
            dataPoints: zip(names, state.bars).map { name, bar in
                AXDataPoint(x: name, y: Double(bar.amountMinor))
            },
        )
        return AXChartDescriptor(
            title: String(localized: "Over time", bundle: .appLanguage),
            summary: GroupStatsCopy.chartDescription(state),
            xAxis: xAxis,
            yAxis: yAxis,
            additionalAxes: [],
            series: [series],
        )
    }
}
