import Shared
import SwiftUI

/// "Settling up": who settles their share of the period's bills fastest and slowest, once two people had one.
struct StatsSettlingSection: View {
    let state: GroupStatsUiState

    var body: some View {
        if let settling = state.settling {
            VStack(alignment: .leading, spacing: 8) {
                StatsSectionTitle(title: "Settling up")
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                VStack(spacing: 0) {
                    StatsSettlerLine(
                        label: String(localized: "Settles fastest"),
                        settler: settling.fastest,
                        currency: state.currency,
                    )
                    Divider().padding(.leading, 64)
                    StatsSettlerLine(
                        label: String(localized: "Slowest to settle"),
                        settler: settling.slowest,
                        currency: state.currency,
                    )
                }
                .background(Palette.card, in: .rect(cornerRadius: 26))
                Text("How long each one takes, on average, to settle their share of the period's bills. What's still open counts until today.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

private struct StatsSettlerLine: View {
    let label: String
    let settler: StatsSettler
    let currency: GroupCurrency

    var body: some View {
        StatsAvatarRow(
            initial: settler.initials,
            color: settler.avatarColor,
            photoUrl: settler.photoUrl,
            label: [label, name, days, open].compactMap { $0 }.joined(separator: ", "),
        ) {
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: label)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Text(verbatim: name)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 1) {
                Text(verbatim: days)
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
                if let open {
                    Text(verbatim: open)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
        }
    }

    private var name: String {
        GroupStatsCopy.name(settler.name, isMe: settler.isMe)
    }

    /// "same day on average" for 0, else "2 days on average".
    private var days: String {
        let days = Int(settler.averageDays)
        return days == 0 ? String(localized: "same day on average") : String(localized: "\(days) days on average")
    }

    /// "$80 still open", nothing once every debt of the period is covered.
    private var open: String? {
        settler.openMinor > 0 ? String(localized: "\(currency.format(settler.openMinor)) still open") : nil
    }
}
