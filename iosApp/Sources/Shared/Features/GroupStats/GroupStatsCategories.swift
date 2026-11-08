import Charts
import Shared
import SwiftUI

/// "By category" (6.6): the ring of slices in each main category's fixed color, the largest in its center and the
/// legend with the percentages, then each main category with its bar. The slice the pointer is over (or its legend
/// line) pops out, the others fade, and the center names it with its share; a tap or a click on a slice, a legend line
/// or a row opens the category's bills ("Expenses by category"). The ring sweeps in when the period changes.
struct StatsCategoriesSection: View {
    let state: GroupStatsUiState
    var pieSize: CGFloat = 132
    /// The main category tapped and what of the card it was.
    let open: (String, StatsCategoryTarget) -> Void

    @State private var selected: Int?
    /// 0 to 1 as the ring sweeps in, clockwise from the top.
    @State private var sweep: Double = 0

    /// What resets the pick and plays the ring again: the period and its categories.
    private var ringKey: String {
        "\(state.periodStartMillis)-\(state.periodEndMillis)-" + state.categories.map { "\($0.category.id):\($0.percent)" }.joined(separator: ",")
    }

    var body: some View {
        if let largest = state.topCategory {
            let top = selected.flatMap { state.categories.indices.contains($0) ? state.categories[$0] : nil } ?? largest
            VStack(alignment: .leading, spacing: 8) {
                StatsSectionTitle(title: "By category")
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                HStack(spacing: 20) {
                    Chart {
                        ForEach(Array(state.categories.enumerated()), id: \.element.category.id) { index, row in
                            SectorMark(
                                angle: .value("Share", Double(row.percent) * sweep),
                                innerRadius: .ratio(0.62),
                                outerRadius: .ratio(selected == index ? 1 : 0.92),
                                angularInset: state.categories.count > 1 ? 1.5 : 0,
                            )
                            .foregroundStyle(row.category.color.container)
                            .opacity(selected == nil || selected == index ? 1 : 0.4)
                        }
                        // The rest of the turn while the ring sweeps in, drawn in no color
                        SectorMark(angle: .value("Share", 100 * (1 - sweep)), innerRadius: .ratio(0.62), outerRadius: .ratio(0.92))
                            .foregroundStyle(.clear)
                    }
                    .chartLegend(.hidden)
                    .chartOverlay { _ in
                        GeometryReader { geometry in
                            Rectangle()
                                .fill(.clear)
                                .contentShape(Rectangle())
                                .onTapGesture { location in
                                    if let index = slice(at: location, in: geometry.size) {
                                        open(state.categories[index].category.id, .ring)
                                    }
                                }
                                .onContinuousHover { phase in
                                    switch phase {
                                    case let .active(location):
                                        selected = slice(at: location, in: geometry.size)
                                    case .ended:
                                        selected = nil
                                    }
                                }
                        }
                    }
                    .frame(width: pieSize, height: pieSize)
                    .overlay {
                        VStack(spacing: 0) {
                            Text(verbatim: "\(top.percent)%")
                                .font(.headline.weight(.bold))
                                .monospacedDigit()
                                .contentTransition(.numericText())
                            Text(verbatim: CategoryCopy.name(top.category))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 24)
                        .allowsHitTesting(false)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Text("Split by category: \(pieDescription)"))
                    VStack(alignment: .leading, spacing: 2) {
                        ForEach(Array(state.categories.enumerated()), id: \.element.category.id) { index, row in
                            Button {
                                open(row.category.id, .legend)
                            } label: {
                                HStack(spacing: 8) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(row.category.color.container)
                                        .frame(width: 12, height: 12)
                                    Text(verbatim: CategoryCopy.name(row.category))
                                        .font(.footnote)
                                        .lineLimit(1)
                                    Text(verbatim: "\(row.percent)%")
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                        .monospacedDigit()
                                }
                                .padding(.vertical, 3)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .opacity(selected == nil || selected == index ? 1 : 0.4)
                            .onHover { isOver in
                                if isOver {
                                    selected = index
                                } else if selected == index {
                                    selected = nil
                                }
                            }
                        }
                    }
                    .accessibilityHidden(true)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 26))
                .animation(.smooth(duration: 0.25), value: selected)
                .onChange(of: ringKey) { selected = nil }
                .task(id: ringKey) {
                    sweep = 0
                    try? await Task.sleep(for: .milliseconds(20))
                    withAnimation(.easeOut(duration: 0.6)) { sweep = 1 }
                }
                VStack(spacing: 0) {
                    ForEach(Array(state.categories.enumerated()), id: \.element.category.id) { index, row in
                        Button {
                            open(row.category.id, .row)
                        } label: {
                            StatsCategoryLine(row: row, currency: state.currency)
                        }
                        .buttonStyle(.plain)
                        .accessibilityHint(Text("See expenses"))
                        if index < state.categories.count - 1 {
                            Divider().padding(.leading, 70)
                        }
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 26))
            }
        }
    }

    /// The slice under a point of the ring, by its angle clockwise from the top; nil deep in the hole or outside it.
    private func slice(at point: CGPoint, in size: CGSize) -> Int? {
        let radius = min(size.width, size.height) / 2
        let dx = point.x - size.width / 2
        let dy = point.y - size.height / 2
        let distance = (dx * dx + dy * dy).squareRoot()
        guard distance <= radius, distance >= radius * 0.4 else { return nil }
        var degrees = atan2(dy, dx) * 180 / .pi + 90
        if degrees < 0 { degrees += 360 }
        let total = Double(state.categories.reduce(0) { $0 + Int($1.percent) })
        guard total > 0 else { return nil }
        var end = 0.0
        for (index, row) in state.categories.enumerated() {
            end += Double(row.percent) / total * 360
            if degrees < end { return index }
        }
        return state.categories.count - 1
    }

    private var pieDescription: String {
        state.categories.map { "\(CategoryCopy.name($0.category)) \($0.percent)%" }.joined(separator: ", ")
    }
}

private struct StatsCategoryLine: View {
    let row: StatsCategoryRow
    let currency: GroupCurrency

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            CategoryBadge(category: row.category)
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text(verbatim: CategoryCopy.name(row.category))
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Text(verbatim: currency.format(row.amountMinor))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                }
                StatsAmountBar(fraction: row.barFraction)
            }
            Image(systemName: "chevron.forward")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
                .frame(maxHeight: .infinity)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(.rect)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(CategoryCopy.name(row.category)): \(Int(row.percent))% of the total, \(currency.format(row.amountMinor))"))
    }
}
