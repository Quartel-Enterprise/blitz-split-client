import Observation
import Shared
import SwiftUI

@Observable
final class ReleaseNotesModel {
    private(set) var state: ReleaseNotesUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createReleaseNotesViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ReleaseNotesUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        // A version's GitHub release, or all of them, in the browser
        watches.add(viewModel.actionWatcher.observe { action in
            guard let open = action as? ReleaseNotesUiActionOpenUrl else { return }
            ExternalURL.open(open.url)
        })
    }

    func send(_ event: any ReleaseNotesUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Release notes", from the profile's "Privacy and about": what each version changed on this platform, newest
/// first, the shared code having picked the versions and their notes. Pushed over the profile on iPhone and iPad,
/// the page's name in the bar, and a sheet on the Mac's Settings with Done. One column, 640 pt at most, centred in
/// a wider window, as the legal texts.
struct ReleaseNotesView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ReleaseNotesModel?

    var body: some View {
        Group {
            if let model {
                ReleaseNotesScreen(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(iOS)
        .navigationTitle("Release notes")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                GitHubButton { model?.send(ReleaseNotesUiEventOnAllReleasesClicked.shared) }
            }
        }
        #else
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") { model?.send(ReleaseNotesUiEventOnBackClicked.shared) }
            }
        }
        .onExitCommand { model?.send(ReleaseNotesUiEventOnBackClicked.shared) }
        .frame(minWidth: 480, idealWidth: 700, minHeight: 400, idealHeight: 620)
        #endif
        .onAppear {
            if model == nil { model = ReleaseNotesModel() }
        }
    }
}

private struct ReleaseNotesScreen: View {
    let model: ReleaseNotesModel

    private var state: ReleaseNotesUiState {
        model.state
    }

    var body: some View {
        VStack(spacing: 0) {
            #if os(macOS)
            // A sheet has no bar to name the page in, whichever state it shows, nor to hold the GitHub button
            HStack(alignment: .center) {
                Text("Release notes")
                    .font(.title2.bold())
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                GitHubButton { model.send(ReleaseNotesUiEventOnAllReleasesClicked.shared) }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .frame(maxWidth: 640, alignment: .leading)
            .frame(maxWidth: .infinity)
            #endif
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Palette.groupedBackground.ignoresSafeArea())
    }

    @ViewBuilder private var content: some View {
        Group {
            if state.showsLoadFailed {
                ContentUnavailableView {
                    Label("Couldn't load the release notes", systemImage: "wifi.exclamationmark")
                } description: {
                    Text("Check your connection and try again.")
                } actions: {
                    Button("Try again") {
                        model.send(ReleaseNotesUiEventOnRetryClicked.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .prominentLabel()
                }
            } else if state.showsSkeleton {
                // A device that never downloaded the notes: their cards shimmer until they arrive or fail
                ScrollView {
                    ReleaseNotesPlaceholder()
                        .shimmering()
                        .releaseNotesColumn()
                }
                .scrollDisabled(true)
            } else {
                ReleaseNotesList(model: model)
            }
        }
    }
}

private struct ReleaseNotesList: View {
    let model: ReleaseNotesModel

    private var state: ReleaseNotesUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if state.showsBetaSwitch {
                    Toggle(
                        "Show beta versions",
                        isOn: Binding(
                            get: { state.showsBeta?.boolValue == true },
                            set: { model.send(ReleaseNotesUiEventOnShowBetaToggled(isOn: $0)) },
                        ),
                    )
                    .toggleStyle(.switch)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Palette.card, in: .rect(cornerRadius: 16))
                }
                if state.showsEmpty {
                    Text(
                        state.hasOnlyBetaReleases
                            ? "There is no stable version yet. Turn on beta versions to see what is being tested."
                                as LocalizedStringKey
                            : "No release notes yet.",
                    )
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                }
                ForEach(state.visibleReleases, id: \.version.fullName) { release in
                    Button {
                        model.send(ReleaseNotesUiEventOnReleaseClicked(version: release.version))
                    } label: {
                        ReleaseCard(release: release)
                    }
                    .buttonStyle(.plain)
                    .hoverHighlight(in: .rect(cornerRadius: 16))
                    .linkPointer()
                    .accessibilityHint("Open on GitHub")
                }
            }
            .releaseNotesColumn()
        }
    }
}

/// A version: its name, "Beta 2" for a beta and "This version" for the installed one, the day it came out and its
/// notes, one bullet each. A tap opens its release on GitHub.
private struct ReleaseCard: View {
    let release: ReleaseNotes

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                // Large text, or a narrow window, drops the badges under the name
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) { heading }
                    VStack(alignment: .leading, spacing: 6) { heading }
                }
                Text(verbatim: date)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(release.notes.enumerated()), id: \.offset) { _, note in
                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text(verbatim: "•")
                            .foregroundStyle(.secondary)
                            .accessibilityHidden(true)
                        Text(verbatim: note)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 16))
        .contentShape(.rect(cornerRadius: 16))
    }

    @ViewBuilder
    private var heading: some View {
        Text(verbatim: release.version.name)
            .font(.title3.bold())
        if let beta = release.version.beta {
            // Int, not KotlinInt's Int32, so the key is the catalog's "%lld"
            ReleaseBadge(text: Text("Beta \(Int(truncating: beta))"), isProminent: false)
        }
        if release.isInstalled {
            ReleaseBadge(text: Text("This version"), isProminent: true)
        }
    }

    /// The day the version came out, long ("November 14, 2026") in the device's own format.
    private var date: String {
        let day = release.date
        return longSiteDate(year: day.year, month: day.month, day: day.day)
    }
}

/// GitHub's mark, opening every release on GitHub: the bar's on iPhone and iPad, the header's on the Mac's sheet.
private struct GitHubButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image("GitHubLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 22, height: 22)
        }
        .linkPointer()
        .accessibilityLabel("Open on GitHub")
        .help("Open on GitHub")
    }
}

/// A card's badge: "This version" in the accent, "Beta 2" quiet beside it.
private struct ReleaseBadge: View {
    let text: Text
    let isProminent: Bool

    var body: some View {
        text
            .font(.caption.weight(.semibold))
            .foregroundStyle(isProminent ? AnyShapeStyle(Palette.onAccent) : AnyShapeStyle(.primary))
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(
                isProminent ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.fill.tertiary),
                in: .capsule,
            )
    }
}

/// The notes still coming: the switch's row and three versions' cards.
private struct ReleaseNotesPlaceholder: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ShimmerBlock(height: 48, cornerRadius: 16)
            ForEach(0..<3, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 12) {
                    ShimmerLine(width: 90, height: 18)
                    ShimmerLine(width: 140, height: 10)
                    ShimmerLine()
                    ShimmerLine()
                    ShimmerLine(width: 220)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 16))
            }
        }
    }
}

private extension View {
    /// The notes' one column: 640 pt at most, centred in a wider window, as the legal texts'.
    func releaseNotesColumn() -> some View {
        padding(.horizontal, 20)
            .padding(.vertical, 16)
            .frame(maxWidth: 640, alignment: .leading)
            .frame(maxWidth: .infinity)
    }
}
