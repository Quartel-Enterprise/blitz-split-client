import Observation
import Shared
import SwiftUI

@Observable
final class JoinInviteModel {
    private(set) var state: JoinInviteUiState

    @ObservationIgnored private let holder: ViewModelHolder<JoinInviteViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteJoinInvite) {
        holder = BlitzSplitApple.shared.createJoinInviteViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! JoinInviteUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any JoinInviteUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite": a group's invite link opened, pushed over the groups, or over the welcome before there is a profile.
/// Offline it keeps the link and opens the invite by itself once the device is online.
struct JoinInviteView: View {
    let route: RouteJoinInvite

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: JoinInviteModel?

    var body: some View {
        Group {
            if let model {
                JoinInviteContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .background(Palette.groupedBackground)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(JoinInviteUiEventOnNotNowClicked.shared)
                }
            }
        }
        .onAppear {
            if model == nil { model = JoinInviteModel(route: route) }
        }
    }
}

private struct JoinInviteContent: View {
    let model: JoinInviteModel

    var body: some View {
        let state = model.state
        ScrollView {
            Group {
                switch state.status {
                case .ready:
                    invite(state)
                case .invalid:
                    problem(
                        systemImage: "xmark.circle",
                        title: "This invite is no longer valid",
                        message: "The link was changed or the group was deleted. Ask whoever invited you for a new link.",
                        displayLink: nil,
                    )
                case .offline:
                    problem(
                        systemImage: "wifi.slash",
                        title: "No internet to open the invite",
                        message: "We kept the link. As soon as the device connects, the invite opens by itself.",
                        displayLink: state.displayLink,
                    )
                default:
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.top, 160)
                }
            }
            .padding(20)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
    }

    private func invite(_ state: JoinInviteUiState) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Invite")
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .glassEffect(.regular.tint(Palette.accent.opacity(0.2)), in: .capsule)
            Group {
                if let creatorName = state.creatorName {
                    Text("\(creatorName) invited you to \(state.groupName)")
                } else {
                    Text("You're invited to \(state.groupName)")
                }
            }
            .font(.largeTitle.bold())
            GroupedCard {
                HStack(spacing: 16) {
                    GroupAvatar(initials: state.groupInitials, color: state.groupColor, size: 56)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(verbatim: state.groupName)
                            .font(.title3.weight(.semibold))
                        summary(state)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }
            }
            if !state.joinedNames.isEmpty {
                HStack(spacing: 12) {
                    faces(state.joined)
                    alreadyIn(state.joinedNames)
                        .font(.subheadline)
                }
            }
            Text("No account needed. On the next screen you pick who you are.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            VStack(spacing: 8) {
                PrimaryButton(title: "Join group") {
                    model.send(JoinInviteUiEventOnJoinClicked.shared)
                }
                Button("Not now") {
                    model.send(JoinInviteUiEventOnNotNowClicked.shared)
                }
                .controlSize(.large)
            }
        }
    }

    /// "4 members · created by Pierre".
    private func summary(_ state: JoinInviteUiState) -> Text {
        let count = Text("\(state.memberCount) members")
        guard let creatorName = state.creatorName else { return count }
        return Text("\(count) · created by \(creatorName)")
    }

    /// The faces of the first three who joined, and how many more there are.
    private func faces(_ members: [JoinMember]) -> some View {
        HStack(spacing: -8) {
            ForEach(members.prefix(3), id: \.id) { member in
                AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 32)
                    .overlay(Circle().stroke(Palette.groupedBackground, lineWidth: 2))
            }
            if members.count > 3 {
                Text(verbatim: "+\(members.count - 3)")
                    .font(.caption.weight(.semibold))
                    .frame(width: 32, height: 32)
                    .background(Palette.card, in: .circle)
            }
        }
        .accessibilityHidden(true)
    }

    /// "Pierre and Ana are already in the group", the names joined the way the language joins them.
    private func alreadyIn(_ names: [String]) -> Text {
        let list = names.formatted(.list(type: .and))
        return names.count == 1 ? Text("\(list) is already in the group") : Text("\(list) are already in the group")
    }

    private func problem(
        systemImage: String,
        title: LocalizedStringKey,
        message: LocalizedStringKey,
        displayLink: String?,
    ) -> some View {
        VStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.system(size: 44))
                .foregroundStyle(Palette.accent)
            Text(title)
                .font(.title2.bold())
                .multilineTextAlignment(.center)
            Text(message)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if let displayLink {
                Text(verbatim: displayLink)
                    .font(.callout.monospaced())
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Palette.card, in: .rect(cornerRadius: 10))
            }
            VStack(spacing: 8) {
                if displayLink != nil {
                    PrimaryButton(title: "Try again") {
                        model.send(JoinInviteUiEventOnRetryClicked.shared)
                    }
                }
                TintedButton(title: "Go to home") {
                    model.send(JoinInviteUiEventOnGoHomeClicked.shared)
                }
            }
            .padding(.top, 8)
        }
        .padding(.top, 48)
    }
}
