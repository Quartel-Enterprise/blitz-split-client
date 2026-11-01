import Observation
import Shared
import SwiftUI

@Observable
final class ContactInviteModel {
    private(set) var state: ContactInviteUiState

    @ObservationIgnored private let holder: ViewModelHolder<ContactInviteViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteContactInvite) {
        holder = BlitzSplitApple.shared.createContactInviteViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ContactInviteUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { action in
            // Share is a ShareLink: a share sheet can't be opened from code
            if let copy = action as? ContactInviteUiActionCopyLink { Pasteboard.copy(copy.link) }
        })
    }

    func send(_ event: any ContactInviteUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite with a link or QR code" (11.4), from Contacts, the agenda ("Invite Helena") or a guest's profile ("Invite
/// Diego", their personal link): an invite is always a group's link, so it asks which, "New group" included. A sheet
/// everywhere (a form sheet with an iPad's room). Lying down it splits in two columns, the QR code beside the choices;
/// on an iPhone Duo open upright the fold runs across, so the QR code, the link and the group sit above it and the
/// choices and buttons below it, as the notebook board draws.
struct ContactInviteView: View {
    let route: RouteContactInvite

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ContactInviteModel?

    private var title: Text {
        if let name = route.name { return Text("Invite \(name)") }
        return Text("Invite with a link or QR code")
    }

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                ContactInviteContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle(title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(ContactInviteUiEventOnCloseClicked.shared)
                }
            }
        }
        #if os(iOS)
        .formSheetSizing()
        #else
        .frame(minWidth: 460, idealWidth: 520, minHeight: 560, idealHeight: 640)
        #endif
        .onAppear {
            if model == nil { model = ContactInviteModel(route: route) }
        }
    }
}

private struct ContactInviteContent: View {
    let model: ContactInviteModel

    /// The sheet's own size, which decides its two columns.
    @State private var layout = ScreenLayout()
    @State private var division: FoldDivision?

    /// The fold of an iPhone Duo open upright, running across the sheet.
    private var horizontalFold: CGRect? {
        guard let division, !division.isVertical else { return nil }
        return division.frame
    }

    var body: some View {
        Group {
            if let fold = horizontalFold {
                VStack(spacing: 0) {
                    InviteQrCard(state: model.state)
                        .padding(.horizontal, 24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                        .frame(height: max(0, fold.minY))
                    Color.clear
                        .frame(height: fold.height)
                    ScrollView {
                        InviteChoices(model: model)
                            .padding(.horizontal, 24)
                            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                            .frame(maxWidth: .infinity)
                    }
                }
            } else if layout.usesTwoColumns {
                ScrollView {
                    TwoColumnsLayout(spacing: 24) {
                        InviteQrCard(state: model.state)
                    } second: {
                        InviteChoices(model: model)
                    }
                    .padding(20)
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        InviteGroupPicker(model: model)
                        InviteQrCard(state: model.state)
                        InviteNotesAndButtons(model: model)
                    }
                    .padding(20)
                    .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
    }
}

/// The group picker, then whom the link is for, the offline notice and the buttons.
private struct InviteChoices: View {
    let model: ContactInviteModel

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            InviteGroupPicker(model: model)
            InviteNotesAndButtons(model: model)
        }
    }
}

/// "The invite is for a group. Pick which.": the groups as chips, the one marked, and "New group" (not for a guest's
/// personal link). With no group yet, only "New group" and why.
private struct InviteGroupPicker: View {
    let model: ContactInviteModel

    private var state: ContactInviteUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(state.hasNoGroups ? "Create a group first: an invite is always a group's link." : "The invite is for a group. Pick which.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(state.groups, id: \.groupId) { group in
                        chip(isSelected: group.groupId == state.selectedGroupId) {
                            Text(verbatim: group.name)
                        } action: {
                            model.send(ContactInviteUiEventOnGroupPicked(groupId: group.groupId))
                        }
                    }
                    if !state.isPersonal {
                        chip(isSelected: false) {
                            Label("New group", systemImage: "plus")
                        } action: {
                            model.send(ContactInviteUiEventOnNewGroupClicked.shared)
                        }
                    }
                }
            }
            .scrollIndicators(.hidden)
            .accessibilityLabel(Text("Group of the invite"))
        }
    }

    private func chip(
        isSelected: Bool,
        @ViewBuilder label: () -> some View,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            label()
                .font(.subheadline.weight(isSelected ? .semibold : .regular))
                .padding(.horizontal, 14)
                .frame(height: 36)
                .foregroundStyle(isSelected ? Palette.onAccent : Color.primary)
                .background(isSelected ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.fill.tertiary), in: .capsule)
                .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// The QR code with the group's name and the link, or a dashed box while the group's link waits for its first sync.
private struct InviteQrCard: View {
    let state: ContactInviteUiState

    var body: some View {
        if let group = state.selectedGroup {
            VStack(spacing: 12) {
                if let qrCode = state.qrCode, let displayLink = state.displayLink {
                    QrCodeImage(qrCode: qrCode)
                        .frame(maxWidth: 220)
                    Text(verbatim: group.name)
                        .font(.headline)
                    Text(verbatim: displayLink)
                        .font(.subheadline.monospaced())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .textSelection(.enabled)
                } else {
                    VStack(spacing: 6) {
                        Text("Link pending")
                            .font(.headline)
                        Text("The link of \(group.name) comes with its next sync.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, minHeight: 160)
                    .overlay {
                        RoundedRectangle(cornerRadius: 20)
                            .strokeBorder(.secondary, style: StrokeStyle(lineWidth: 1.5, dash: [8, 6]))
                    }
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity)
            .background(Palette.card, in: .rect(cornerRadius: 26))
            .accessibilityElement(children: .contain)
        }
    }
}

/// Who the link is for, the offline notice, then Copy link and Share.
private struct InviteNotesAndButtons: View {
    let model: ContactInviteModel

    private var state: ContactInviteUiState {
        model.state
    }

    private var whoJoins: String {
        if let name = state.name {
            return state.isPersonal
                ? String(localized: "\(name) joins right away as themselves.")
                : String(localized: "\(name) picks their name on the list or joins as a new member.")
        }
        return String(localized: "Whoever opens the link picks their name on the group's list or joins as a new member.")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if state.selectedGroup != nil {
                Text(verbatim: whoJoins)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if state.showsOfflineNotice {
                Label("Offline · the link already exists, so you can copy and share it.", systemImage: "wifi.slash")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 10) {
                Button {
                    model.send(ContactInviteUiEventOnCopyClicked.shared)
                } label: {
                    Group {
                        if state.isLinkCopied {
                            Label("Copied", systemImage: "checkmark")
                        } else {
                            Label("Copy link", systemImage: "doc.on.doc")
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .accessibilityAddTraits(.updatesFrequently)
                if let link = state.link, let group = state.selectedGroup {
                    ShareLink(item: link, message: ContactsCopy.inviteMessage(groupName: group.name, name: state.name)) {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Button {} label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(true)
                }
            }
            .controlSize(.large)
            .buttonBorderShape(.capsule)
            .disabled(state.link == nil)
        }
    }
}
