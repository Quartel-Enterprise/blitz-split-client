import Shared
import SwiftUI

/// "Time to settle up": the group's reminder arrived or passed and this person owes, until they dismiss it or settle.
/// It comes back with the next occurrence. Over the tabs, under the bar.
struct ReminderBannerView: View {
    let banner: ReminderBanner
    let onSettle: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Time to settle up")
                        .font(.headline)
                    Text(verbatim: ReminderCopy.schedule(banner.reminder))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            } icon: {
                Image(systemName: "bell.badge.fill")
                    .foregroundStyle(Palette.accent)
            }
            .accessibilityElement(children: .combine)
            HStack(spacing: 8) {
                Button("Settle up", action: onSettle)
                    .buttonStyle(.borderedProminent)
                    .prominentLabel()
                Button("Dismiss", action: onDismiss)
                    .buttonStyle(.bordered)
            }
            .controlSize(.small)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 18))
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .accessibilityElement(children: .contain)
    }
}
