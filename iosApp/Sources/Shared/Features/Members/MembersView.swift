import Observation
import Shared
import SwiftUI

@Observable
final class MembersModel {
    private(set) var state: MembersUiState
    private(set) var comingSoonCount = 0
    private(set) var changeErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<MembersViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteMembers) {
        holder = BlitzSplitApple.shared.createMembersViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! MembersUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is MembersUiActionShowComingSoon: self?.comingSoonCount += 1
            case is MembersUiActionShowChangeError: self?.changeErrorCount += 1
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

/// "Members": who is in the group and who joined, pushed over the groups. A tap on a person opens their options:
/// rename, a personal invite (coming soon) and removal, which only someone who hasn't joined allows.
struct MembersView: View {
    let route: RouteMembers

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: MembersModel?
    @State private var toast: LocalizedStringKey?
    @State private var hidesToast: Task<Void, Never>?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                MembersList(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Members")
        .navigationSubtitle(subtitle)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Add person", systemImage: "person.badge.plus") {
                    model?.send(MembersUiEventOnAddClicked.shared)
                }
                .disabled(model?.state.isLoaded != true)
            }
        }
        .sheet(isPresented: editorShown) {
            if let model {
                MemberNameSheet(model: model)
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
        .overlay(alignment: .top) {
            if let toast {
                Text(toast)
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .glassEffect(.regular, in: .capsule)
                    .padding(.top, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        // The counts only grow on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .onChange(of: model?.comingSoonCount ?? 0) { show("Coming soon") }
        .onChange(of: model?.changeErrorCount ?? 0) { show("Couldn't save the change. Try again.") }
        .onAppear {
            if model == nil { model = MembersModel(route: route) }
        }
    }

    /// "Viagem Floripa · 2 of 4 joined".
    private var subtitle: Text {
        guard let state = model?.state, state.isLoaded else { return Text(verbatim: "") }
        return Text(verbatim: "\(state.groupName) · ") + Text("\(state.joinedCount) of \(state.memberCount) joined")
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

    /// A second action keeps the toast for two seconds from that one.
    private func show(_ text: LocalizedStringKey) {
        withAnimation { toast = text }
        hidesToast?.cancel()
        hidesToast = Task {
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else { return }
            withAnimation { toast = nil }
        }
    }
}

private struct MembersList: View {
    let model: MembersModel

    private var state: MembersUiState {
        model.state
    }

    var body: some View {
        List {
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
                }
            }
            Section("People") {
                ForEach(state.members, id: \.id) { member in
                    MemberRow(member: member) { model.send($0) }
                }
            }
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #endif
        .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
        .frame(maxWidth: .infinity)
        .background(Palette.groupedBackground)
    }
}

private struct MemberRow: View {
    let member: MembersMember
    let send: (any MembersUiEvent) -> Void

    var body: some View {
        Menu {
            Button("Rename", systemImage: "pencil") {
                send(MembersUiEventOnRenameClicked(memberId: member.id))
            }
            if member.canBeInvited {
                Button("Personal invite", systemImage: "link") {
                    send(MembersUiEventOnPersonalInviteClicked(memberId: member.id))
                }
            }
            if !member.isMe {
                Divider()
                Button(role: .destructive) {
                    send(MembersUiEventOnRemoveClicked(memberId: member.id))
                } label: {
                    Label {
                        Text("Remove from group")
                        if member.hasJoined {
                            Text("\(member.name) already joined the group")
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
                if member.hasJoined {
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
