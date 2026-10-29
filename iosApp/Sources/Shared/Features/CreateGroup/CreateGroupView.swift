import Observation
import Shared
import SwiftUI

@Observable
final class CreateGroupModel {
    private(set) var state: CreateGroupUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createCreateGroupViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! CreateGroupUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: a field reads
    /// its capped text back at once, so a 41st character never shows for a frame.
    var current: CreateGroupUiState {
        (holder.viewModel.uiState.value as? CreateGroupUiState) ?? state
    }

    func send(_ event: any CreateGroupUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "New group": a sheet on iPhone, iPad and Mac, with Cancel and Create (✓) in the bar. The group's avatar
/// changes color on a tap, Type and Currency are menus, and the members are added by name.
struct CreateGroupView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws, and a
    /// ViewModel made for nothing would still read the profile.
    @State private var model: CreateGroupModel?

    var body: some View {
        Group {
            if let model {
                CreateGroupForm(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("New group")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model?.send(CreateGroupUiEventOnCloseClicked.shared)
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Create", systemImage: "checkmark") {
                    model?.send(CreateGroupUiEventOnCreateClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(model?.state.canCreate != true)
            }
        }
        .interactiveDismissDisabled(model?.state.isCreating == true)
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 500, minHeight: 560, idealHeight: 680)
        #endif
        .onAppear {
            if model == nil { model = CreateGroupModel() }
        }
    }
}

private struct CreateGroupForm: View {
    let model: CreateGroupModel

    private var state: CreateGroupUiState {
        model.state
    }

    var body: some View {
        Form {
            #if os(macOS)
            // A Mac sheet has no title bar, so the title heads the form
            Section {
                Text("New group").font(.title2.bold())
            }
            .listRowBackground(Color.clear)
            #endif
            Section {
                Button {
                    model.send(CreateGroupUiEventOnColorClicked.shared)
                } label: {
                    GroupAvatar(initials: state.initials, color: state.color, size: 88)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Change the group's color")
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
            Section {
                TextField("Group name", text: nameBinding)
                Picker("Type", selection: typeBinding) {
                    ForEach(state.types, id: \.self) { type in
                        Text(type.title).tag(type)
                    }
                }
                Picker("Currency", selection: currencyBinding) {
                    ForEach(state.currencies, id: \.self) { currency in
                        Text(verbatim: currency.label).tag(currency)
                    }
                }
            }
            MembersSection(model: model)
        }
        .formStyle(.grouped)
    }

    private var nameBinding: Binding<String> {
        Binding(
            get: { model.current.name },
            set: { model.send(CreateGroupUiEventOnNameChanged(name: $0)) },
        )
    }

    private var typeBinding: Binding<GroupType> {
        Binding(
            get: { model.state.type },
            set: { model.send(CreateGroupUiEventOnTypeSelected(type: $0)) },
        )
    }

    private var currencyBinding: Binding<GroupCurrency> {
        Binding(
            get: { model.state.currency },
            set: { model.send(CreateGroupUiEventOnCurrencySelected(currency: $0)) },
        )
    }
}

/// "Who's in": the creator first ("you", not removable), then each name added. The draft row adds on
/// Return, and a repeated name swaps the hint below for the nicknames to pick from.
private struct MembersSection: View {
    let model: CreateGroupModel

    private var state: CreateGroupUiState {
        model.state
    }

    var body: some View {
        Section {
            ForEach(state.members, id: \.id) { member in
                MemberRow(member: member) {
                    model.send(CreateGroupUiEventOnRemoveMemberClicked(memberId: member.id))
                }
            }
            HStack(spacing: 12) {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
                    .foregroundStyle(state.takenName == nil ? Palette.accent : Palette.negative)
                    .accessibilityHidden(true)
                TextField("Add by name", text: draftBinding)
                    #if os(iOS)
                    .textInputAutocapitalization(.words)
                    #endif
                    .submitLabel(.done)
                    .onSubmit { model.send(CreateGroupUiEventOnAddMemberClicked.shared) }
                if state.canAddMember {
                    Button("Add") { model.send(CreateGroupUiEventOnAddMemberClicked.shared) }
                        .buttonStyle(.borderless)
                }
            }
        } header: {
            HStack {
                Text("Who's in")
                Spacer()
                Text("\(state.members.count) people")
            }
        } footer: {
            if let takenName = state.takenName {
                VStack(alignment: .leading, spacing: 8) {
                    Label {
                        Text("\(takenName) is already in the group. Use a nickname to tell them apart.")
                    } icon: {
                        Image(systemName: "exclamationmark.circle")
                    }
                    .foregroundStyle(Palette.negative)
                    HStack(spacing: 8) {
                        ForEach(state.nicknameSuggestions, id: \.self) { suggestion in
                            Button("Use “\(suggestion)”") {
                                model.send(CreateGroupUiEventOnSuggestionClicked(name: suggestion))
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
            }
            if state.showsNoAppNotice {
                Text("Nobody needs the app right now. You can already add expenses for everyone and send the link later.")
            }
        }
    }

    private var draftBinding: Binding<String> {
        Binding(
            get: { model.current.draft },
            set: { model.send(CreateGroupUiEventOnDraftChanged(draft: $0)) },
        )
    }
}

private struct MemberRow: View {
    let member: CreateGroupMember
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: member.initials, color: member.avatarColor, photoUrl: nil, size: 32)
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: member.name)
                if member.isMe {
                    Text("you")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            if !member.isMe {
                Button("Remove \(member.name)", systemImage: "xmark.circle.fill", action: onRemove)
                    .labelStyle(.iconOnly)
                    .foregroundStyle(.secondary)
                    .buttonStyle(.borderless)
            }
        }
    }
}

/// A group's avatar: its initials on one of the palette's colors, in a rounded square so a group never
/// reads as a person's circle.
struct GroupAvatar: View {
    let initials: String
    let color: AvatarColor
    let size: CGFloat

    var body: some View {
        Text(verbatim: initials)
            .font(.system(size: size * 0.32, weight: .heavy))
            .foregroundStyle(color.content)
            .frame(width: size, height: size)
            .background(color.container, in: .rect(cornerRadius: size * 0.32))
    }
}

extension GroupType {
    var title: LocalizedStringKey {
        switch self {
        case .home: "Home"
        case .event: "Event"
        case .other: "Other"
        default: "Trip"
        }
    }
}

extension GroupCurrency {
    /// "Brazilian Real (R$)", named by the system in the device's language.
    var label: String {
        let locale = Locale.current
        let name = locale.localizedString(forCurrencyCode: code) ?? code
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .currency
        formatter.currencyCode = code
        let symbol = formatter.currencySymbol ?? code
        return "\(name.prefix(1).uppercased())\(name.dropFirst()) (\(symbol))"
    }
}
