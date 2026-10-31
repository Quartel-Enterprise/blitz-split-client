import Observation
import Shared
import SwiftUI

@Observable
final class JoinPickModel {
    private(set) var state: JoinPickUiState
    private(set) var joinErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<JoinPickViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteJoinPick) {
        holder = BlitzSplitApple.shared.createJoinPickViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! JoinPickUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is JoinPickUiActionShowJoinError { self?.joinErrorCount += 1 }
        })
    }

    /// The state as the ViewModel holds it right now: a text field reads its capped value back in the same keystroke.
    var current: JoinPickUiState {
        (holder.viewModel.uiState.value as? JoinPickUiState) ?? state
    }

    func send(_ event: any JoinPickUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Who are you in the group?": the members nobody joined as yet to pick from, the ones who joined, and
/// "I'm not on the list", which joins under a new name.
struct JoinPickView: View {
    let route: RouteJoinPick

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: JoinPickModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                JoinPickList(model: model)
                    .safeAreaInset(edge: .bottom) {
                        joinButton(model.state)
                    }
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Palette.groupedBackground)
            }
        }
        .navigationTitle("Who are you in the group?")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .sheet(isPresented: newMemberShown) {
            if let model {
                NewMemberSheet(model: model)
            }
        }
        // The count only grows on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .glassToast(
            "Couldn't join. Check your connection and try again.",
            trigger: model?.joinErrorCount ?? 0,
            duration: .seconds(3)
        )
        .onAppear {
            if model == nil { model = JoinPickModel(route: route) }
        }
    }

    private func joinButton(_ state: JoinPickUiState) -> some View {
        Group {
            if let selectedName = state.selectedName {
                PrimaryButton(title: "Join as \(selectedName)", isEnabled: state.canJoin, isLoading: state.isJoining) {
                    model?.send(JoinPickUiEventOnJoinClicked.shared)
                }
            } else {
                PrimaryButton(title: "Choose a name", isEnabled: false) {}
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
        .frame(maxWidth: .infinity)
    }

    /// Dismissing the sheet (a swipe down, Esc) closes it in the ViewModel too.
    private var newMemberShown: Binding<Bool> {
        Binding(
            get: { model?.state.newMember != nil },
            set: { shown in
                if !shown { model?.send(JoinPickUiEventOnNewMemberDismissed.shared) }
            },
        )
    }
}

private struct JoinPickList: View {
    let model: JoinPickModel

    var body: some View {
        let state = model.state
        List {
            Section {
                Text("Pick your name. You'll see the expenses already added for that person.")
                    .foregroundStyle(.secondary)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }
            if let takenName = state.takenName {
                Section {
                    Label {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(takenName) just joined from another device")
                                .font(.headline)
                            if let creatorName = state.creatorName {
                                Text("If it was you, talk to \(creatorName).")
                            } else {
                                Text("If it was you, talk to whoever invited you.")
                            }
                        }
                    } icon: {
                        Image(systemName: "exclamationmark.circle.fill")
                            .foregroundStyle(.orange)
                    }
                }
            }
            if !state.notJoined.isEmpty {
                Section("Haven't joined yet") {
                    ForEach(state.notJoined, id: \.id) { member in
                        Button {
                            model.send(JoinPickUiEventOnMemberSelected(memberId: member.id))
                        } label: {
                            MemberLine(member: member, isSelected: member.id == state.selectedMemberId)
                        }
                        .buttonStyle(.plain)
                        .disabled(state.isJoining)
                        .accessibilityAddTraits(member.id == state.selectedMemberId ? .isSelected : [])
                    }
                }
            }
            Section {
                Button("I'm not on the list", systemImage: "person.badge.plus") {
                    model.send(JoinPickUiEventOnNotInListClicked.shared)
                }
                .disabled(state.isJoining)
            }
            if !state.joined.isEmpty {
                Section("Already in the group") {
                    ForEach(state.joined, id: \.id) { member in
                        MemberLine(member: member, isSelected: false)
                    }
                }
            }
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #endif
        .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
        .frame(maxWidth: .infinity)
        .background(Palette.groupedBackground)
    }
}

private struct MemberLine: View {
    let member: JoinMember
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: member.name)
                    .foregroundStyle(member.hasJoined ? .secondary : .primary)
                if member.hasJoined {
                    Text("Already joined")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            if !member.hasJoined {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(isSelected ? Palette.accent : .secondary)
                    .font(.title3)
                    .accessibilityHidden(true)
            }
        }
        .contentShape(.rect)
    }
}

/// "Join with another name": someone new to the group, under a name nobody in it has.
private struct NewMemberSheet: View {
    let model: JoinPickModel

    @FocusState private var isFocused: Bool

    var body: some View {
        if let draft = model.state.newMember {
            NavigationStack {
                Form {
                    Section {
                        TextField("Your name", text: nameBinding)
                            .focused($isFocused)
                            .submitLabel(.join)
                            .onSubmit { model.send(JoinPickUiEventOnNewMemberConfirmed.shared) }
                    } header: {
                        Text("You join as a new member. Expenses added before don't include you.")
                            .textCase(nil)
                    } footer: {
                        Text("At least 2 letters, and not a name already in the group.")
                            .foregroundStyle(draft.isNameTaken ? .red : .secondary)
                    }
                }
                .navigationTitle("Join with another name")
                #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel", systemImage: "xmark", role: .cancel) {
                            model.send(JoinPickUiEventOnNewMemberDismissed.shared)
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Join group", systemImage: "checkmark") {
                            model.send(JoinPickUiEventOnNewMemberConfirmed.shared)
                        }
                        .disabled(!model.state.canJoinAsNewMember)
                    }
                }
            }
            .presentationDetents([.medium])
            #if os(macOS)
            .frame(minWidth: 380, minHeight: 240)
            #endif
            .onAppear { isFocused = true }
        }
    }

    private var nameBinding: Binding<String> {
        Binding(
            get: { model.current.newMember?.name ?? "" },
            set: { model.send(JoinPickUiEventOnNewNameChanged(name: $0)) },
        )
    }
}
