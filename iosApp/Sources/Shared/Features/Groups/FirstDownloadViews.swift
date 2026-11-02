import Shared
import SwiftUI

/// A bar that fills as a new device's first download brings the groups (5.1d), and runs until the server said how
/// many are coming.
struct DownloadProgressBar: View {
    let progress: DownloadProgress

    var body: some View {
        if let fraction = progress.fraction {
            ProgressView(value: fraction.doubleValue)
                .progressViewStyle(.linear)
        } else {
            ProgressView()
                .progressViewStyle(.linear)
        }
    }
}

/// "Bringing your groups" in Overall balance's place while the first download brings the groups down one by one, with
/// "3 of 7 groups" and its bar once the server said how many are coming (5.1d).
struct GroupsFirstDownloadCard: View {
    let progress: DownloadProgress

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Bringing your groups")
                .font(.headline)
            DownloadProgressBar(progress: progress)
            Text(verbatim: GroupsCopy.firstDownloadHint(progress))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Palette.card, in: .rect(cornerRadius: 28))
        .accessibilityElement(children: .combine)
    }
}

/// A grey row holding the place of a group still coming down.
struct GroupSkeletonRow: View {
    var body: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 44 * 0.32)
                .fill(.fill.tertiary)
                .frame(width: 44, height: 44)
            VStack(alignment: .leading, spacing: 6) {
                Capsule().fill(.fill.tertiary).frame(width: 140, height: 12)
                Capsule().fill(.fill.quaternary).frame(width: 100, height: 10)
            }
            Spacer()
            Capsule().fill(.fill.tertiary).frame(width: 64, height: 12)
        }
        .padding(.vertical, 4)
        .accessibilityHidden(true)
    }
}

extension GroupsCopy {
    /// "Syncing… 3 of 7 groups" once the first download knows how many are coming, else "Syncing…".
    static func syncing(_ progress: DownloadProgress?) -> String {
        guard let progress, let groupCount = progress.groupCount?.intValue else {
            return String(localized: "Syncing…")
        }
        let arrived = Int(progress.arrivedGroupCount)
        return groupCount == 1
            ? String(localized: "Syncing… \(arrived) of 1 group")
            : String(localized: "Syncing… \(arrived) of \(groupCount) groups")
    }

    /// "3 of 7 groups. You can use the app meanwhile: what has arrived shows below."
    static func firstDownloadHint(_ progress: DownloadProgress) -> String {
        guard let groupCount = progress.groupCount?.intValue else {
            return String(localized: "You can use the app meanwhile: what has arrived shows below.")
        }
        let arrived = Int(progress.arrivedGroupCount)
        return groupCount == 1
            ? String(localized: "\(arrived) of 1 group. You can use the app meanwhile: what has arrived shows below.")
            : String(
                localized: "\(arrived) of \(groupCount) groups. You can use the app meanwhile: what has arrived shows below.",
            )
    }
}
