import Observation
import PhotosUI
import Shared
import SwiftUI
#if os(iOS)
import UIKit
#endif

@Observable
final class EditProfileModel {
    private(set) var state: EditProfileUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createEditProfileViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! EditProfileUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: the name field reads
    /// what it sent back at once, so the Mac's capital first letter never lands on a later keystroke.
    var current: EditProfileUiState {
        (holder.viewModel.uiState.value as? EditProfileUiState) ?? state
    }

    func send(_ event: any EditProfileUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    func photoPicked(_ data: Data?) {
        guard let data else {
            send(EditProfileUiEventOnPhotoUnreadable.shared)
            return
        }
        send(EditProfileUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
    }

    isolated deinit {
        holder.clear()
    }
}

/// The editor, left once saved: a sheet over the profile with Cancel (✕) and Save (✓), since a photo, a name and a
/// notice are too little for a screen (Pierre's call, beta feedback). On an iPhone it opens at half height and grows to
/// the whole screen, the avatar and its color beside the name lying down; with an iPad's room (an iPad, an iPhone Duo
/// open) it is a 540 pt form sheet, one column. The Mac edits the profile inline (`EditProfileSections`).
struct EditProfileView: View {
    @State private var model = EditProfileModel()
    /// The sheet's own size, which decides the columns.
    @State private var layout = ScreenLayout()
    /// The window under the sheet, not the sheet: whether it has an iPad's room.
    @Environment(\.windowLayout) private var windowLayout

    var body: some View {
        Group {
            if layout.usesTwoColumns, !windowLayout.isLarge {
                TwoColumnsLayout(firstWidth: 300, spacing: 32) {
                    Form { EditProfileAvatarSections(model: model) }
                        .formStyle(.grouped)
                } second: {
                    Form { EditProfileNameSections(model: model, isInline: false) }
                        .formStyle(.grouped)
                }
            } else {
                Form {
                    EditProfileSections(model: model, isInline: false)
                }
                .formStyle(.grouped)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .navigationTitle("Edit profile")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model.send(EditProfileUiEventOnBackClicked.shared)
                }
                .disabled(model.state.isSaving)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", systemImage: "checkmark") {
                    model.send(EditProfileUiEventOnSaveClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
                .disabled(!model.state.canSave)
            }
        }
        .interactiveDismissDisabled(model.state.isSaving || model.state.hasUnsavedInput)
        .photoCrop(model.state.photoCrop, frame: .circle) { model.send(EditProfileUiEventOnPhotoCrop(event: $0)) }
        #if os(iOS)
        // Half height on an iPhone, where the keyboard lifts it with the name field; lying down the sheet takes the
        // whole screen whatever the detents. The canvas' 540 pt form sheet with an iPad's room
        .presentationDetents(windowLayout.isLarge ? [.large] : [.medium, .large])
        .formSheetSizing()
        #endif
    }
}

/// The editor's sections. Inline (the Mac's profile), Save and Cancel show under the name once something changed.
struct EditProfileSections: View {
    let model: EditProfileModel
    let isInline: Bool

    var body: some View {
        EditProfileAvatarSections(model: model)
        EditProfileNameSections(model: model, isInline: isInline)
    }
}

/// The photo and, while there is none, the avatar's color: the first column lying down.
private struct EditProfileAvatarSections: View {
    let model: EditProfileModel

    var body: some View {
        let state = model.state
        Section {
            if state.isLoaded {
                PhotoEditor(model: model)
            } else {
                // The saved profile not read yet: never "?" on the default color
                ShimmerCircle(size: 96)
                    .shimmering()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
        }
        // Why: the color only shows behind the initial, so it is offered while there is no photo.
        if state.isLoaded, state.showsAvatarColors {
            Section {
                HStack {
                    ForEach(state.avatarColors, id: \.self) { color in
                        ColorSwatch(color: color, isSelected: color == state.avatarColor) {
                            model.send(EditProfileUiEventOnAvatarColorSelected(color: color))
                        }
                        if color != state.avatarColors.last { Spacer(minLength: 8) }
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel("Avatar color")
            } header: {
                Text("Avatar color")
            } footer: {
                Text("Shows when you have no photo.")
            }
            .animation(.snappy, value: state.avatarColor)
        }
    }
}

/// The name, where it came from and who sees it: the second column lying down.
private struct EditProfileNameSections: View {
    let model: EditProfileModel
    let isInline: Bool

    @ViewBuilder
    var body: some View {
        if model.state.isLoaded {
            sections
        } else {
            // The saved name not read yet: no empty field that the name would then replace under what was typed
            Section {
                ShimmerBlock(height: 22, cornerRadius: 6)
                    .shimmering()
            }
        }
    }

    @ViewBuilder
    private var sections: some View {
        let state = model.state
        Section {
            TextField("Your name", text: nameBinding.firstLetterCapitalized)
                .textContentType(.name)
                .capitalizesFirstLetter(isPersonName: true)
                .submitLabel(.done)
                .onSubmit { model.send(EditProfileUiEventOnSaveClicked.shared) }
        } header: {
            Text("Your name")
        } footer: {
            HStack {
                if let error = nameError(state) {
                    Text(error)
                        .foregroundStyle(Palette.negative)
                }
                Spacer()
                Text(verbatim: "\(state.nameLength)/\(state.nameMaxLength)")
                    .monospacedDigit()
            }
        }
        if let provider = state.accountProvider {
            Section {
                Label {
                    Text("Name and photo came from your \(provider.displayName) account. Changing them here only applies to Blitz Split and doesn't change your \(provider.displayName) account.")
                        .font(.subheadline)
                } icon: {
                    Image(systemName: "info.circle")
                }
            }
        }
        Section {
            if isInline, state.hasChanges {
                HStack {
                    Spacer()
                    Button("Cancel") { model.send(EditProfileUiEventOnDiscardClicked.shared) }
                    Button("Save") { model.send(EditProfileUiEventOnSaveClicked.shared) }
                        .buttonStyle(.glassProminent)
                        .prominentLabel()
                        .disabled(!state.canSave)
                }
            }
        } footer: {
            Text("Your groups and anyone with your profile link see your name and photo.")
        }
    }

    private var nameBinding: Binding<String> {
        Binding(
            get: { model.current.name },
            set: { model.send(EditProfileUiEventOnNameChanged(name: $0)) },
        )
    }

    private func nameError(_ state: EditProfileUiState) -> LocalizedStringKey? {
        switch state.nameError {
        case .tooShort: "Type at least \(Int(state.nameMinLength)) letters."
        case .tooLong: "Use up to \(Int(state.nameMaxLength)) characters."
        default: nil
        }
    }
}

/// The avatar with "Edit photo": take one (iPhone and iPad), choose one, or remove it.
private struct PhotoEditor: View {
    let model: EditProfileModel

    @State private var showsPhotoPicker = false
    @State private var takesPhoto = false

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            avatar
            .overlay {
                if state.isPreparingPhoto { ProgressView() }
            }
            .overlay(alignment: .bottomTrailing) {
                Menu {
                    #if os(iOS)
                    if CameraPicker.isAvailable {
                        Button("Take photo", systemImage: "camera") { takesPhoto = true }
                    }
                    #endif
                    // Why: the system picker needs no photo library permission and returns one image, or nothing.
                    Button("Choose photo", systemImage: "photo.on.rectangle") { showsPhotoPicker = true }
                    if let provider = state.accountPhotoProvider {
                        Button("Use \(provider.displayName) account photo", systemImage: "person.crop.circle") {
                            model.send(EditProfileUiEventOnAccountPhotoSelected.shared)
                        }
                    }
                    if state.hasPhoto {
                        Button("Remove photo", systemImage: "trash", role: .destructive) {
                            model.send(EditProfileUiEventOnPhotoRemoved.shared)
                        }
                    }
                } label: {
                    CornerGlassIcon(systemName: "pencil")
                }
                .menuStyle(.borderlessButton)
                .menuIndicator(.hidden)
                .fixedSize()
                .offset(x: 4 + CornerGlassIcon.targetPadding, y: 4 + CornerGlassIcon.targetPadding)
                .accessibilityLabel("Edit photo")
            }
            if state.showsPhotoError {
                Text("Couldn't use this photo. Try another one.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .photoLibraryPicker(isPresented: $showsPhotoPicker) { data in model.photoPicked(data) }
        #if os(iOS)
        .cameraCapture(isRequested: $takesPhoto) { data in model.photoPicked(data) }
        #endif
    }

    @ViewBuilder
    private var avatar: some View {
        let state = model.state
        let photo = AvatarPhoto(
            initial: state.avatarInitial,
            color: state.avatarColor,
            photoUrl: state.photoUrl,
            size: 96,
        )
        #if os(macOS)
        // Ajustes draw the form inside the profile, where the avatar opens the photo big, as Bible Planner's
        Button {
            model.send(EditProfileUiEventOnAvatarClicked.shared)
        } label: {
            photo.contentShape(.circle)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("View photo")
        #else
        photo.accessibilityHidden(true)
        #endif
    }
}
