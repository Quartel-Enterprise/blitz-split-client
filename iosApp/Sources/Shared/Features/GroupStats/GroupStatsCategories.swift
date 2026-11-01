import Charts
import Shared
import SwiftUI

/// "By category" (6.6): the ring of slices in each main category's fixed color, the largest in its center and the
/// legend with the percentages, then each main category with its bar and its subcategories.
struct StatsCategoriesSection: View {
    let state: GroupStatsUiState
    var pieSize: CGFloat = 132

    var body: some View {
        if let top = state.topCategory {
            VStack(alignment: .leading, spacing: 8) {
                StatsSectionTitle(title: "By category")
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                HStack(spacing: 20) {
                    Chart(state.categories, id: \.category.id) { row in
                        SectorMark(
                            angle: .value("Share", Int(row.percent)),
                            innerRadius: .ratio(0.68),
                            angularInset: state.categories.count > 1 ? 1.5 : 0,
                        )
                        .foregroundStyle(row.category.color.container)
                    }
                    .chartLegend(.hidden)
                    .frame(width: pieSize, height: pieSize)
                    .overlay {
                        VStack(spacing: 0) {
                            Text(verbatim: "\(top.percent)%")
                                .font(.headline.weight(.bold))
                                .monospacedDigit()
                            Text(verbatim: CategoryCopy.name(top.category))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 24)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Text("Split by category: \(pieDescription)"))
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(state.categories, id: \.category.id) { row in
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
                        }
                    }
                    .accessibilityHidden(true)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 26))
                VStack(spacing: 0) {
                    ForEach(Array(state.categories.enumerated()), id: \.element.category.id) { index, row in
                        StatsCategoryLine(row: row, currency: state.currency)
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
                StatsAmountBar(fraction: row.barFraction)
                if !row.subcategories.isEmpty {
                    Text(verbatim: row.subcategories.map { "\(CategoryCopy.name($0.category)) \(currency.format($0.amountMinor))" }.joined(separator: " · "))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(CategoryCopy.name(row.category)): \(Int(row.percent))% of the total, \(currency.format(row.amountMinor))"))
    }
}
