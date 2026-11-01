import Observation
import Shared
import SwiftUI

@Observable
final class JoinInviteModel {
    private(set) var state: JoinInviteUiState
    private(set) var joinErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<JoinInviteViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteJoinInvite) {
        holder = BlitzSplitApple.shared.createJoinInviteViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! JoinInviteUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is JoinInviteUiActionShowJoinError { self?.joinErrorCount += 1 }
        })
    }

    func send(_ event: any JoinInviteUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite": a group's invite link opened, pushed over the groups, or over the welcome before there is a profile; on
/// the Mac one sheet over the window, "Who are you" pushed inside it (3.1, 3.2).
/// Offline it keeps the link and opens the invite by itself once the device is online. A personal link joins straight
/// in as the member it was made for, or goes on to the list with "I'm not Carla".
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
        // The count only grows on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .glassToast(
            "Couldn't join. Check your connection and try again.",
            trigger: model?.joinErrorCount ?? 0,
            duration: .seconds(3)
        )
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 460, minHeight: 440, idealHeight: 560)
        #endif
        .onAppear {
            if model == nil { model = JoinInviteModel(route: route) }
        }
    }
}

/// One column on an iPhone upright and with an iPad's room upright; lying down (an iPhone, an iPad, an iPhone Duo
/// open) the title and what to expect on the left (290 pt, 360 pt with an iPad's room) and the group and its buttons
/// on the right, in a 900 pt container (3.1). The Mac's sheet keeps one column.
private struct JoinInviteContent: View {
    let model: JoinInviteModel

    /// The screen's own size, which decides the columns.
    @State private var layout = ScreenLayout()

    var body: some View {
        let state = model.state
        Group {
            if layout.usesTwoColumns, state.status != .loading {
                TwoColumnsLayout(
                    firstWidth: layout.isLarge ? 360 : 290,
                    spacing: layout.isLarge ? 56 : 44,
                    maxWidth: 900,
                    alignment: .center,
                ) {
                    ScrollView {
                        firstColumn(state)
                            .padding(.vertical, 20)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .defaultScrollAnchor(.center, for: .alignment)
                } second: {
                    ScrollView {
                        secondColumn(state)
                            .padding(.vertical, 20)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .defaultScrollAnchor(.center, for: .alignment)
                }
                .padding(.horizontal, layout.isLarge ? 64 : 20)
            } else {
                ScrollView {
                    column(state)
                        .padding(20)
                        .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .measuringScreenLayout($layout)
    }

    @ViewBuilder
    private func column(_ state: JoinInviteUiState) -> some View {
        switch state.status {
        case .ready:
            VStack(alignment: .leading, spacing: 20) {
                heading(state)
                group(state)
                personalRow(state)
                hint(state)
                buttons(state)
            }
        case .invalid, .offline:
            VStack(spacing: 16) {
                problemText(state, isCentered: true)
                problemButtons(state)
                    .padding(.top, 8)
            }
            .padding(.top, 48)
        default:
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 160)
        }
    }

    /// Lying down: the title and what to expect, or what went wrong.
    @ViewBuilder
    private func firstColumn(_ state: JoinInviteUiState) -> some View {
        if state.status == .ready {
            VStack(alignment: .leading, spacing: 14) {
                heading(state)
                hint(state)
            }
        } else {
            VStack(alignment: .leading, spacing: 16) {
                problemText(state, isCentered: false)
            }
        }
    }

    /// Lying down: the group and its buttons, or the buttons of what went wrong.
    @ViewBuilder
    private func secondColumn(_ state: JoinInviteUiState) -> some View {
        if state.status == .ready {
            VStack(alignment: .leading, spacing: 16) {
                group(state)
                personalRow(state)
                buttons(state)
            }
        } else {
            problemButtons(state)
        }
    }

    /// "Invite" and "Pierre invited you to Viagem Floripa", or for a link made for one member "Personal invite" and
    /// "Pierre invited you as Carla".
    @ViewBuilder
    private func heading(_ state: JoinInviteUiState) -> some View {
        if state.isPersonal, let member = state.personalMember {
            chip("Personal invite")
            Group {
                if let inviterName = state.inviterName {
                    Text("\(inviterName) invited you as \(member.name)")
                } else {
                    Text("You're joining as \(member.name)")
                }
            }
            .font(.largeTitle.bold())
        } else {
            chip("Invite")
            Group {
                if let creatorName = state.creatorName {
                    Text("\(creatorName) invited you to \(state.groupName)")
                } else {
                    Text("You're invited to \(state.groupName)")
                }
            }
            .font(.largeTitle.bold())
        }
    }

    @ViewBuilder
    private func hint(_ state: JoinInviteUiState) -> some View {
        Group {
            if state.isPersonal, let member = state.personalMember {
                Text("This link is just for you. You join as \(member.name), without picking from the list.")
            } else {
                Text("No account needed. On the next screen you pick who you are.")
            }
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
    }

    /// The Mac shows the member a personal link joins as, the only row on its list.
    @ViewBuilder
    private func personalRow(_ state: JoinInviteUiState) -> some View {
        #if os(macOS)
        if state.isPersonal, let member = state.personalMember {
            GroupedCard {
                HStack(spacing: 12) {
                    AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 32)
                    Text(verbatim: member.name)
                        .font(.headline)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
        }
        #endif
    }

    /// "Join group" and "Not now", or for a personal link "Join as Carla", which joins straight in, and "I'm not Carla".
    @ViewBuilder
    private func buttons(_ state: JoinInviteUiState) -> some View {
        VStack(spacing: 8) {
            if state.isPersonal, let member = state.personalMember {
                PrimaryButton(title: "Join as \(member.name)", isEnabled: !state.isJoining, isLoading: state.isJoining) {
                    model.send(JoinInviteUiEventOnJoinClicked.shared)
                }
                Button("I'm not \(member.name)") {
                    model.send(JoinInviteUiEventOnNotMeClicked.shared)
                }
                .controlSize(.large)
                .disabled(state.isJoining)
            } else {
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

    private func chip(_ title: LocalizedStringKey) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .glassEffect(.regular.tint(Palette.accent.opacity(0.2)), in: .capsule)
    }

    /// The group's card and the faces of who already joined.
    @ViewBuilder
    private func group(_ state: JoinInviteUiState) -> some View {
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
    }

    /// "4 members · created by Pierre".
    private func summary(_ state: JoinInviteUiState) -> Text {
        let count = Text("\(Int(state.memberCount)) members")
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

    /// What went wrong: a link no longer valid, or no internet to read it (with the link kept).
    @ViewBuilder
    private func problemText(_ state: JoinInviteUiState, isCentered: Bool) -> some View {
        let alignment: TextAlignment = isCentered ? .center : .leading
        Image(systemName: state.status == .offline ? "wifi.slash" : "xmark.circle")
            .font(.system(size: 44))
            .foregroundStyle(Palette.accent)
        Group {
            if state.status == .offline {
                Text("No internet to open the invite")
            } else {
                Text("This invite is no longer valid")
            }
        }
        .font(.title2.bold())
        .multilineTextAlignment(alignment)
        Group {
            if state.status == .offline {
                Text("We kept the link. As soon as the device connects, the invite opens by itself.")
            } else if state.isPersonal {
                Text("Someone already joined through this link, or it was changed. Ask whoever sent it for a new one.")
            } else {
                Text("The link was changed or the group was deleted. Ask whoever invited you for a new link.")
            }
        }
        .foregroundStyle(.secondary)
        .multilineTextAlignment(alignment)
        if state.status == .offline {
            Text(verbatim: state.displayLink)
                .font(.callout.monospaced())
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Palette.card, in: .rect(cornerRadius: 10))
        }
    }

    /// "Try again" while offline, "Go to home", and "Scan another QR code" for a link no longer valid.
    private func problemButtons(_ state: JoinInviteUiState) -> some View {
        let isOffline = state.status == .offline
        return VStack(spacing: 8) {
            if isOffline {
                PrimaryButton(title: "Try again") {
                    model.send(JoinInviteUiEventOnRetryClicked.shared)
                }
            }
            TintedButton(title: "Go to home") {
                model.send(JoinInviteUiEventOnGoHomeClicked.shared)
            }
            if !isOffline {
                Button("Scan another QR code", systemImage: "qrcode.viewfinder") {
                    model.send(JoinInviteUiEventOnScanAnotherClicked.shared)
                }
                .font(.headline)
                .frame(minHeight: 44)
            }
        }
    }
}
