import Shared
import SwiftUI

/// A bar that fills as a new device's first download brings the groups (5.1d), and runs until the server said how
/// many are coming.
struct DownloadProgressBar: View {
    let progress: DownloadProgress

    var body: some View {
        if progress.isWaitingForInternet {
            // Offline nothing comes, so the bar stops where it was
            ProgressView(value: progress.fraction?.doubleValue ?? 0)
                .progressViewStyle(.linear)
        } else if let fraction = progress.fraction {
            ProgressView(value: fraction.doubleValue)
                .progressViewStyle(.linear)
        } else {
            ProgressView()
                .progressViewStyle(.linear)
        }
    }
}

/// "Bringing your groups" in Overall balance's place while the first download brings the groups down one by one, with
/// "3 of 7 groups" and its bar once the server said how many are coming (5.1d). Offline, "No internet. Your groups
/// show up once you're back online." over the stopped bar instead, since nothing comes.
struct GroupsFirstDownloadCard: View {
    let progress: DownloadProgress

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if progress.isWaitingForInternet {
                WaitingForInternetNote(isAboutGroups: true)
                DownloadProgressBar(progress: progress)
            } else {
                Text("Bringing your groups")
                    .font(.headline)
                DownloadProgressBar(progress: progress)
                Text(verbatim: GroupsCopy.firstDownloadHint(progress))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Palette.card, in: .rect(cornerRadius: 28))
        .accessibilityElement(children: .combine)
    }
}

extension GroupsCopy {
    /// "Syncing… 3 of 7 groups" once the first download knows how many are coming, else "Syncing…".
    static func syncing(_ progress: DownloadProgress?) -> String {
        if progress?.isWaitingForInternet == true {
            return String(localized: "No internet. Your groups show up once you're back online.")
        }
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
