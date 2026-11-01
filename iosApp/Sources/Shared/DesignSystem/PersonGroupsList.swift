import Shared
import SwiftUI

/// "By group": what each group adds to the net with someone, then the net, "Net, already offset" over several groups
/// and "Total" for one. Balances opens it in a row, and Record payment shows it when settling up across groups.
struct PersonGroupsList: View {
    let groups: [PersonGroupBalance]
    let currency: GroupCurrency

    private var netMinor: Int64 {
        groups.reduce(0) { $0 + $1.amountMinor }
    }

    private var netLabel: LocalizedStringKey {
        groups.count > 1 ? "Net, already offset" : "Total"
    }

    private func direction(_ amountMinor: Int64) -> LocalizedStringKey {
        amountMinor < 0 ? "you owe" : "owes you"
    }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(groups, id: \.groupId) { part in
                HStack(spacing: 10) {
                    GroupAvatar(initials: part.groupInitials, color: part.groupColor, size: 28)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(verbatim: part.groupName)
                            .lineLimit(1)
                        Text(direction(part.amountMinor))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 8)
                    Text(verbatim: currency.formatSigned(part.amountMinor))
                        .monospacedDigit()
                        .foregroundStyle(part.amountMinor < 0 ? Palette.negative : Palette.accent)
                }
                .font(.subheadline)
                .padding(.vertical, 8)
                Divider()
            }
            HStack {
                Text(netLabel)
                Spacer()
                Text(verbatim: currency.formatSigned(netMinor))
                    .monospacedDigit()
                    .foregroundStyle(netMinor < 0 ? Palette.negative : Palette.accent)
            }
            .font(.subheadline.weight(.semibold))
            .padding(.vertical, 9)
        }
        .padding(.horizontal, 14)
        .background(Palette.groupedBackground, in: .rect(cornerRadius: 16))
    }
}
