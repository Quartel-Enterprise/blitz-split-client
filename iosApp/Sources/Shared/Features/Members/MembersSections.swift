import Observation
import Shared
import SwiftUI

@Observable
final class MembersModel {
    private(set) var state: MembersUiState
    private(set) var changeErrorCount = 0
    private(set) var releaseErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<MembersViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteGroupDetail) {
        holder = BlitzSplitApple.shared.createMembersViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! MembersUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is MembersUiActionShowChangeError: self?.changeErrorCount += 1
            case is MembersUiActionShowReleaseError: self?.releaseErrorCount += 1
            case let copy as MembersUiActionCopyLink: Pasteboard.copy(copy.link)
            // The personal invite's ShareLink shares the link itself: a share sheet can't be opened from code.
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: the name field
    /// reads its capped text back at once, so a 41st character never shows for a frame.
    var current: MembersUiState {
        (holder.viewModel.uiState.value as? MembersUiState) ?? state
    }

    func send(_ event: any MembersUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Members", the group's tab under its header: the group's link and who is in it and who joined. A tap on a person
/// opens their options: rename, a personal invite, which joins straight in as someone nobody joined as yet, and
/// removal, which only someone who hasn't joined and owes or is owed nothing allows. The group's creator may also
/// release the name of someone who joined, online only. Each person shows their balance. Nothing until Room is read.
struct MembersSections: View {
    let model: MembersModel
    /// The group's screen in the shared back stack, whose Share anchors the Mac's invite popover on this tab.
    let host: AppRoute

    private var state: MembersUiState {
        model.state
    }

    var body: some View {
        if state.isLoaded {
            Section {
                HStack(spacing: 12) {
                    Image(systemName: "link")
                        .foregroundStyle(Palette.accent)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Invite link")
                        Group {
                            if let displayLink = state.displayLink {
                                Text(verbatim: displayLink)
                            } else {
                                Text("Not created yet")
                            }
                        }
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                    }
                    Spacer()
                    Button("Share") {
                        model.send(MembersUiEventOnShareClicked.shared)
                    }
                    .buttonStyle(.glassProminent)
                    .buttonBorderShape(.capsule)
                    .invitePopover(host: host)
                }
            }
            Section("People") {
                ForEach(state.members, id: \.id) { member in
                    MemberRow(
                        member: member,
                        currency: state.currency,
                        isReleasing: state.isReleasing && state.memberToRelease?.id == member.id,
                    ) { model.send($0) }
                }
            }
        }
    }
}

extension View {
    /// What Members opens over the group: the name editor and the personal invite as sheets, the removal and release
    /// confirmations, and its toasts.
    func membersOverlays(_ model: MembersModel?) -> some View {
        modifier(MembersOverlays(model: model))
    }
}

private struct MembersOverlays: ViewModifier {
    let model: MembersModel?

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: editorShown) {
                if let model {
                    MemberNameSheet(model: model)
                }
            }
            .sheet(isPresented: personalInviteShown) {
                if let model {
                    PersonalInviteSheet(model: model)
                }
            }
            .alert(removeTitle, isPresented: removalShown, presenting: model?.state.memberToRemove) { member in
                Button("Remove", role: .destructive) {
                    model?.send(MembersUiEventOnRemoveConfirmed.shared)
                }
                Button("Cancel", role: .cancel) {
                    model?.send(MembersUiEventOnRemoveDismissed.shared)
                }
            } message: { member in
                Text("\(member.name) hasn't joined yet. You can add them again later.")
            }
            .alert(releaseTitle, isPresented: releaseShown, presenting: model?.state.memberToRelease) { member in
                Button("Release", role: .destructive) {
                    model?.send(MembersUiEventOnReleaseConfirmed.shared)
                }
                Button("Cancel", role: .cancel) {
                    model?.send(MembersUiEventOnReleaseDismissed.shared)
                }
            } message: { member in
                Text("Whoever joined as \(member.name) leaves the group. \(member.name)'s name, color and bills stay, and \(member.name) can be picked again through the group's link.")
            }
            // The count only grows on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
            .glassToast("Couldn't save the change. Try again.", trigger: model?.changeErrorCount ?? 0)
            .glassToast(
                "Couldn't release the name. Check your connection and try again.",
                trigger: model?.releaseErrorCount ?? 0,
            )
    }

    /// Dismissing the sheet (a swipe down, Esc) closes the editor in the ViewModel too.
    private var editorShown: Binding<Bool> {
        Binding(
            get: { model?.state.editor != nil },
            set: { shown in
                if !shown { model?.send(MembersUiEventOnEditorDismissed.shared) }
            },
        )
    }

    /// Dismissing the sheet (a swipe down, Esc) closes the personal invite in the ViewModel too.
    private var personalInviteShown: Binding<Bool> {
        Binding(
            get: { model?.state.personalInvite != nil },
            set: { shown in
                if !shown { model?.send(MembersUiEventOnPersonalInviteDismissed.shared) }
            },
        )
    }

    private var removeTitle: Text {
        guard let member = model?.state.memberToRemove else { return Text(verbatim: "") }
        return Text("Remove \(member.name) from the group?")
    }

    /// Dismissing the alert (Esc, a tap outside on iPad) closes the confirmation in the ViewModel too.
    private var removalShown: Binding<Bool> {
        Binding(
            get: { model?.state.memberToRemove != nil },
            set: { shown in
                if !shown { model?.send(MembersUiEventOnRemoveDismissed.shared) }
            },
        )
    }

    private var releaseTitle: Text {
        guard let member = model?.state.memberToRelease else { return Text(verbatim: "") }
        return Text("Release \(member.name)'s name?")
    }

    /// An alert closes on any of its buttons, so once "Release" is on its way the member's row shows the wait instead.
    private var releaseShown: Binding<Bool> {
        Binding(
            get: { model?.state.memberToRelease != nil && model?.state.isReleasing == false },
            set: { shown in
                if !shown { model?.send(MembersUiEventOnReleaseDismissed.shared) }
            },
        )
    }
}

private struct MemberRow: View {
    let member: MembersMember
    let currency: GroupCurrency
    /// "Release" waits for the server here, since the alert is gone by then.
    let isReleasing: Bool
    let send: (any MembersUiEvent) -> Void

    var body: some View {
        Menu {
            Button("Rename", systemImage: "pencil") {
                send(MembersUiEventOnRenameClicked(memberId: member.id))
            }
            // Disabled until the sync brings the member's link.
            if member.canBeInvited {
                Button {
                    send(MembersUiEventOnPersonalInviteClicked(memberId: member.id))
                } label: {
                    #if os(macOS)
                    Label("Personal invite for \(member.name)…", systemImage: "link")
                    #else
                    Label("Personal invite for \(member.name)", systemImage: "link")
                    #endif
                }
                .disabled(!member.isPersonalInviteReady)
            }
            if !member.isMe {
                Divider()
                if member.canBeReleased {
                    Button {
                        send(MembersUiEventOnReleaseClicked(memberId: member.id))
                    } label: {
                        #if os(macOS)
                        Label("Release name…", systemImage: "lock.open")
                        #else
                        Label("Release name", systemImage: "lock.open")
                        #endif
                    }
                }
                Button(role: .destructive) {
                    send(MembersUiEventOnRemoveClicked(memberId: member.id))
                } label: {
                    Label {
                        Text("Remove from group")
                        // Why it is disabled
                        if member.hasJoined {
                            Text("\(member.name) already joined the group")
                        } else if member.hasOpenBalance {
                            Text("\(member.name) has an open balance")
                        }
                    } icon: {
                        Image(systemName: "trash")
                    }
                }
                .disabled(!member.canBeRemoved)
            }
        } label: {
            HStack(spacing: 12) {
                AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 32)
                VStack(alignment: .leading, spacing: 2) {
                    if member.isMe {
                        Text("\(member.name) (you)")
                    } else {
                        Text(verbatim: member.name)
                    }
                    Group {
                        if member.isMe {
                            Text("This \(DeviceName.current)")
                        } else if member.hasJoined {
                            Text("Joined")
                        } else {
                            Text("Hasn't joined yet")
                        }
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                }
                Spacer()
                if let balance = member.balance {
                    MemberBalanceText(balance: balance, currency: currency)
                        .font(.footnote.weight(.semibold))
                }
                if isReleasing {
                    ProgressView()
                        .controlSize(.small)
                } else if member.hasJoined {
                    Image(systemName: "checkmark")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Palette.accent)
                        .accessibilityHidden(true)
                }
            }
            .foregroundStyle(.primary)
            .contentShape(.rect)
        }
        .menuIndicator(.hidden)
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Options for \(member.name)"))
    }
}

/// "+$223.50 to get back" in the accent, "−$118.00 owes" in the negative color, "$0.00" when what they get back and
/// owe cancel out, or a muted "Settled up".
private struct MemberBalanceText: View {
    let balance: MemberBalance
    let currency: GroupCurrency

    var body: some View {
        let amount = currency.formatSigned(balance.netMinor)
        if balance.isSettled {
            Text("Settled up")
                .foregroundStyle(.secondary)
        } else if balance.netMinor > 0 {
            Text("\(amount) to get back")
                .monospacedDigit()
                .foregroundStyle(Palette.accent)
        } else if balance.netMinor < 0 {
            Text("\(amount) owes")
                .monospacedDigit()
                .foregroundStyle(Palette.negative)
        } else {
            Text(verbatim: amount)
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
    }
}

/// "Add person" and "Rename" ask the same thing: a name free in the group, with nicknames when it isn't.
private struct MemberNameSheet: View {
    let model: MembersModel

    @FocusState private var isFocused: Bool

    var body: some View {
        if let editor = model.state.editor {
            NavigationStack {
                Form {
                    TextField("Name", text: draftBinding)
                        .focused($isFocused)
                        .submitLabel(.done)
                        .onSubmit { model.send(MembersUiEventOnSaveClicked.shared) }
                    if let takenName = editor.takenName {
                        Section {
                            Text("\(takenName) is already in the group. Use a nickname to tell them apart.")
                                .foregroundStyle(.red)
                            ForEach(editor.nicknameSuggestions, id: \.self) { suggestion in
                                Button("Use “\(suggestion)”") {
                                    model.send(MembersUiEventOnSuggestionClicked(name: suggestion))
                                }
                            }
                        }
                    }
                }
                .navigationTitle(editor.isAdding ? "Add person" : "Rename")
                #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel", systemImage: "xmark", role: .cancel) {
                            model.send(MembersUiEventOnEditorDismissed.shared)
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(editor.isAdding ? "Add" : "Save", systemImage: "checkmark") {
                            model.send(MembersUiEventOnSaveClicked.shared)
                        }
                        .disabled(!editor.canSave)
                    }
                }
            }
            .presentationDetents([.medium])
            #if os(macOS)
            .frame(minWidth: 360, minHeight: 220)
            #endif
            .onAppear { isFocused = true }
        }
    }

    private var draftBinding: Binding<String> {
        Binding(
            get: { model.current.editor?.draft ?? "" },
            set: { model.send(MembersUiEventOnDraftChanged(draft: $0)) },
        )
    }
}

/// "Personal invite": a link that joins straight in as someone nobody joined as yet, and works once. A form sheet
/// with the person's avatar on iPhone and iPad, a sheet on the window with its buttons in a row on the Mac.
private struct PersonalInviteSheet: View {
    let model: MembersModel

    var body: some View {
        if let invite = model.state.personalInvite {
            #if os(macOS)
            MacPersonalInvite(invite: invite, groupName: model.state.groupName) { model.send($0) }
            #else
            PhonePersonalInvite(invite: invite, groupName: model.state.groupName) { model.send($0) }
            #endif
        }
    }
}

/// "Join Viagem Floripa as Carla on Blitz Split:", which the link follows.
private func personalShareMessage(groupName: String, memberName: String) -> Text {
    Text("Join \(groupName) as \(memberName) on Blitz Split:")
}

private func personalInviteText(memberName: String) -> Text {
    Text("Whoever opens this link joins straight in as \(memberName), without picking from the list. It works once.")
}

#if os(iOS)
private struct PhonePersonalInvite: View {
    let invite: PersonalInvite
    let groupName: String
    let send: (any MembersUiEvent) -> Void

    private var member: MembersMember {
        invite.member
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 6) {
                        AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 60)
                        Text(verbatim: member.name)
                            .font(.title2.bold())
                        personalInviteText(memberName: member.name)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    HStack(spacing: 8) {
                        Text(verbatim: invite.displayLink)
                            .font(.body.monospaced())
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .textSelection(.enabled)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button {
                            send(MembersUiEventOnPersonalLinkCopyClicked.shared)
                        } label: {
                            if invite.isLinkCopied {
                                Label("Copied", systemImage: "checkmark")
                            } else {
                                Label("Copy", systemImage: "doc.on.doc")
                            }
                        }
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                        .accessibilityAddTraits(.updatesFrequently)
                    }
                    .padding(.leading, 16)
                    .padding(.trailing, 6)
                    .padding(.vertical, 6)
                    .background(.fill.tertiary, in: .capsule)
                    ShareLink(item: invite.link, message: personalShareMessage(groupName: groupName, memberName: member.name)) {
                        Label("Share with \(member.name)", systemImage: "square.and.arrow.up")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.glassProminent)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
                }
                .padding(20)
            }
            .navigationTitle("Personal invite")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        send(MembersUiEventOnPersonalInviteDismissed.shared)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
#endif

#if os(macOS)
private struct MacPersonalInvite: View {
    let invite: PersonalInvite
    let groupName: String
    let send: (any MembersUiEvent) -> Void

    private var member: MembersMember {
        invite.member
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 40)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Personal invite for \(member.name)")
                        .font(.headline)
                    Text(verbatim: groupName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            personalInviteText(memberName: member.name)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Text(verbatim: invite.displayLink)
                .font(.body.monospaced())
                .lineLimit(1)
                .truncationMode(.middle)
                .textSelection(.enabled)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.background, in: .rect(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).strokeBorder(.separator))
            HStack(spacing: 8) {
                Spacer()
                Button("Close") {
                    send(MembersUiEventOnPersonalInviteDismissed.shared)
                }
                .keyboardShortcut(.cancelAction)
                ShareLink(item: invite.link, message: personalShareMessage(groupName: groupName, memberName: member.name)) {
                    Text("Share…")
                }
                Button {
                    send(MembersUiEventOnPersonalLinkCopyClicked.shared)
                } label: {
                    if invite.isLinkCopied {
                        Text("Copied")
                    } else {
                        Text("Copy link")
                    }
                }
                .buttonStyle(.borderedProminent)
                .keyboardShortcut(.defaultAction)
                .accessibilityAddTraits(.updatesFrequently)
            }
            .controlSize(.large)
            .padding(.top, 4)
        }
        .padding(20)
        .frame(width: 400)
    }
}
#endif
