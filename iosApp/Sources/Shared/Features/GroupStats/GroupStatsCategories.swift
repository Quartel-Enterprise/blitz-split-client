import Shared
import SwiftUI

/// "By category" (6.6): the ring of slices in each main category's fixed color, the largest in its center and the
/// legend with the percentages, then each main category with its bar. The slice the pointer is over (or its legend
/// line) pops out, the others fade, and the center names it with its share; a tap or a click on a slice, a legend line
/// or a row opens All expenses with that category, the group and the period picked. The ring sweeps in when the period changes, and shows
/// whole at once with Reduce Motion on.
struct StatsCategoriesSection: View {
    let state: GroupStatsUiState
    var pieSize: CGFloat = 132
    /// The main category tapped and what of the card it was.
    let open: (String, StatsCategoryTarget) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var selected: Int?

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
                    StatsRing(
                        slices: state.categories.map {
                            StatsRingSlice(id: $0.category.id, percent: Double($0.percent), color: $0.category.color.data)
                        },
                        size: pieSize,
                        ringKey: ringKey,
                        label: Text("Split by category: \(pieDescription)"),
                        selected: $selected,
                        onTap: { index, _ in open(state.categories[index].category.id, .ring) },
                    ) {
                        StatsRingCenter(percent: top.percent, name: CategoryCopy.name(top.category))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        ForEach(Array(state.categories.enumerated()), id: \.element.category.id) { index, row in
                            Button {
                                open(row.category.id, .legend)
                            } label: {
                                HStack(spacing: 8) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(row.category.color.data)
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
                    // The legend fades with the pick; the ring animates its own slices
                    .animation(reduceMotion ? nil : .smooth(duration: 0.25), value: selected)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 26))
                // The picked slice pops out at once with Reduce Motion, as the web's does
                .onChange(of: ringKey) { selected = nil }
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
                StatsAmountBar(fraction: row.barFraction, color: row.category.color.data)
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
