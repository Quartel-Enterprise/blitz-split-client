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
/// "I'm not on the list", which joins under a new name. Lying down (an iPhone, an iPad, an iPhone Duo open) what
/// picking does and "I'm not on the list" take a column on the left, the names and "Join as Bruno" the right (3.2).
struct JoinPickView: View {
    let route: RouteJoinPick

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: JoinPickModel?
    /// The screen's own size, which decides the columns.
    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if let model, model.state.isLoaded, layout.usesTwoColumns {
                TwoColumnsLayout(
                    firstWidth: layout.isLarge ? 360 : 290,
                    spacing: layout.isLarge ? 56 : 40,
                    maxWidth: 900,
                ) {
                    ScrollView {
                        JoinPickIntro(model: model)
                            .padding(.vertical, 20)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                } second: {
                    JoinPickList(model: model, isListOnly: true)
                        .safeAreaInset(edge: .bottom) {
                            joinButton(model.state)
                        }
                }
                .padding(.horizontal, layout.isLarge ? 64 : 0)
                .background(Palette.groupedBackground)
            } else if let model, model.state.isLoaded {
                JoinPickList(model: model, isListOnly: false)
                    .safeAreaInset(edge: .bottom) {
                        joinButton(model.state)
                    }
            } else {
                // The group's names not read yet: they shimmer in place
                ShimmerScreen {
                    ShimmerLine(width: 220, height: 22)
                    ShimmerList(count: 5, showsTrailing: false)
                }
                .background(Palette.groupedBackground)
            }
        }
        .navigationTitle("Who are you in the group?")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #else
        // Pushed inside the invite's sheet, as Default split inside Edit group's: its own Back (Esc), the one way back
        // to the invite short of joining
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Back") {
                    model?.send(JoinPickUiEventOnBackClicked.shared)
                }
                // Popping clears the ViewModel, which would cancel any join in flight, an account's own included
                .disabled(model?.state.isJoining == true)
            }
        }
        #endif
        .sheet(isPresented: newMemberShown) {
            if let model {
                NewMemberSheet(model: model)
                    .foldSheetPlacement()
            }
        }
        // The count only grows on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .glassToast(
            "Couldn't join. Check your connection and try again.",
            trigger: model?.joinErrorCount ?? 0,
            duration: .seconds(3)
        )
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 480, minHeight: 440, idealHeight: 560)
        #endif
        .measuringScreenLayout($layout)
        .onAppear {
            if model == nil { model = JoinPickModel(route: route) }
        }
    }

    private func joinButton(_ state: JoinPickUiState) -> some View {
        Group {
            if let selectedName = state.selectedName {
                PrimaryButton(title: "Join as \(selectedName)", isEnabled: state.canJoin, isLoading: state.showsJoinProgress) {
                    model?.send(JoinPickUiEventOnJoinClicked.shared)
                }
                #if os(macOS)
                .keyboardShortcut(.defaultAction)
                #endif
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

/// Lying down, the left column: what picking a name does, who took the picked name first, and "I'm not on the list".
private struct JoinPickIntro: View {
    let model: JoinPickModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 16) {
            Text("Pick your name. You'll see the expenses already added for that person.")
                .foregroundStyle(.secondary)
            if let takenName = state.takenName {
                TakenNotice(takenName: takenName, creatorName: state.creatorName)
                    .padding(14)
                    .background(Palette.card, in: .rect(cornerRadius: 20))
            }
            Button {
                model.send(JoinPickUiEventOnNotInListClicked.shared)
            } label: {
                Label {
                    Text("I'm not on the list")
                } icon: {
                    NotInListIcon(state: state)
                }
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 36)
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .controlSize(.large)
            .disabled(state.isJoining)
        }
    }
}

/// "Bruno just joined from another device", and who can release the name.
private struct TakenNotice: View {
    let takenName: String
    let creatorName: String?

    var body: some View {
        Label {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(takenName) just joined from another device")
                    .font(.headline)
                if let creatorName {
                    Text("If it was you, ask \(creatorName) to release the name in Members.")
                } else {
                    Text("If it was you, talk to whoever invited you.")
                }
            }
        } icon: {
            Image(systemName: "exclamationmark.circle.fill")
                .foregroundStyle(Palette.negative)
        }
    }
}

/// The names to pick from and who already joined; upright also what picking does and "I'm not on the list", which
/// lying down sit in the column beside it (`isListOnly`).
private struct JoinPickList: View {
    let model: JoinPickModel
    let isListOnly: Bool

    var body: some View {
        let state = model.state
        List {
            if !isListOnly {
                Section {
                    Text("Pick your name. You'll see the expenses already added for that person.")
                        .foregroundStyle(.secondary)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets())
                }
                if let takenName = state.takenName {
                    Section {
                        TakenNotice(takenName: takenName, creatorName: state.creatorName)
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
            if !isListOnly {
                Section {
                    Button {
                        model.send(JoinPickUiEventOnNotInListClicked.shared)
                    } label: {
                        Label {
                            Text("I'm not on the list")
                        } icon: {
                            NotInListIcon(state: state)
                        }
                    }
                    .disabled(state.isJoining)
                }
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

/// A Google or Apple account joins from "I'm not on the list" at once, with its name and photo, so the tap shows it.
private struct NotInListIcon: View {
    let state: JoinPickUiState

    var body: some View {
        if state.showsNotInListProgress {
            ProgressView()
        } else {
            Image(systemName: "person.badge.plus")
        }
    }
}

private struct MemberLine: View {
    let member: JoinMember
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: member.photoUrl, size: 32)
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

/// "Join with another name": someone new to the group, under any name, another member's too. While the join is on its
/// way the check spins and the sheet stays, with nothing else to tap (beta feedback 3, item 30).
private struct NewMemberSheet: View {
    let model: JoinPickModel

    @FocusState private var isFocused: Bool

    var body: some View {
        if model.state.newMember != nil {
            NavigationStack {
                Form {
                    Section {
                        TextField("Your name", text: nameBinding)
                            .focused($isFocused)
                            .disabled(model.state.isJoiningAsNewMember)
                            .submitLabel(.join)
                            .onSubmit { model.send(JoinPickUiEventOnNewMemberConfirmed.shared) }
                    } header: {
                        Text("You join as a new member. Expenses added before don't include you.")
                            .textCase(nil)
                    } footer: {
                        Text("At least 2 letters.")
                            .foregroundStyle(.secondary)
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
                        .disabled(model.state.isJoiningAsNewMember)
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        if model.state.isJoiningAsNewMember {
                            ProgressView()
                        } else {
                            Button("Join group", systemImage: "checkmark") {
                                model.send(JoinPickUiEventOnNewMemberConfirmed.shared)
                            }
                            .disabled(!model.state.canJoinAsNewMember)
                        }
                    }
                }
            }
            .presentationDetents([.medium])
            .interactiveDismissDisabled(model.state.isJoiningAsNewMember)
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
