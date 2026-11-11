import Observation
import Shared
import SwiftUI

@Observable
final class ProfileLinkModel {
    private(set) var state: ProfileLinkUiState

    @ObservationIgnored private let holder: ViewModelHolder<ProfileLinkViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteProfileLink) {
        holder = BlitzSplitApple.shared.createProfileLinkViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ProfileLinkUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any ProfileLinkUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// A profile link opened, blitzsplit.com/u/<code>, while it asks the server or when it has no contact to open: the
/// person's own link opens the profile (Settings on the Mac), and a contact's their contact profile, in its place.
/// One centered column at every size, since it holds a few lines at most.
struct ProfileLinkView: View {
    let route: RouteProfileLink

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ProfileLinkModel?

    var body: some View {
        Group {
            if let model {
                content(model)
            } else {
                Color.clear
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground.ignoresSafeArea())
        .navigationTitle(Text(verbatim: ""))
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        #endif
        .onAppear {
            if model == nil { model = ProfileLinkModel(route: route) }
        }
    }

    @ViewBuilder
    private func content(_ model: ProfileLinkModel) -> some View {
        let state = model.state
        switch state.stage {
        case .stranger:
            if let name = state.name, let color = state.avatarColor {
                ScrollView {
                    VStack(spacing: 12) {
                        AvatarPhoto(initial: state.initials ?? "", color: color, photoUrl: state.photoUrl, size: 112)
                        Text(verbatim: name)
                            .font(.title.bold())
                            .multilineTextAlignment(.center)
                            .accessibilityAddTraits(.isHeader)
                        Text("You don't have any groups with \(name) yet.")
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                        if state.canInvite {
                            Button("Invite to a group", systemImage: "person.badge.plus") {
                                model.send(ProfileLinkUiEventOnInviteClicked.shared)
                            }
                            .buttonStyle(.glassProminent)
                            .prominentLabel()
                            .padding(.top, 8)
                        }
                    }
                    .frame(maxWidth: 480)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 48)
                    .frame(maxWidth: .infinity)
                }
            }
        case .unreachable:
            ContentUnavailableView {
                Label("Couldn't open this profile", systemImage: "wifi.slash")
            } description: {
                Text("Check your connection. It opens by itself once you're back online.")
            } actions: {
                Button("Try again") {
                    model.send(ProfileLinkUiEventOnRetryClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
            }
        case .notFound:
            ContentUnavailableView {
                Label("Profile not found", systemImage: "link")
            } description: {
                Text("This link doesn't open any profile. Ask for it again.")
            }
        default:
            ProgressView("Opening the profile")
        }
    }
}
