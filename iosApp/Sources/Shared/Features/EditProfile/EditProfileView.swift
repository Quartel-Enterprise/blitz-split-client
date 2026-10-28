import Observation
import PhotosUI
import Shared
import SwiftUI

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

/// iPhone and iPad push the editor and leave it once saved.
struct EditProfileView: View {
    @State private var model = EditProfileModel()

    var body: some View {
        Form {
            EditProfileSections(model: model, isInline: false)
        }
        .formStyle(.grouped)
        .navigationTitle("Edit profile")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { model.send(EditProfileUiEventOnSaveClicked.shared) }
                    .disabled(!model.state.canSave)
            }
        }
    }
}

/// The editor's sections. Inline (the Mac's profile), Save and Cancel show under the name once something changed.
struct EditProfileSections: View {
    let model: EditProfileModel
    let isInline: Bool

    var body: some View {
        let state = model.state
        Section {
            PhotoEditor(model: model)
        }
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
        Section {
            TextField(
                "Your name",
                text: Binding(
                    get: { model.state.name },
                    set: { model.send(EditProfileUiEventOnNameChanged(name: $0)) },
                ),
            )
            .textContentType(.name)
            .submitLabel(.done)
            .onSubmit { model.send(EditProfileUiEventOnSaveClicked.shared) }
        } header: {
            Text("Your name")
        } footer: {
            HStack {
                Text(nameHint(state))
                    .foregroundStyle(state.nameError != nil ? Palette.negative : .secondary)
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
                        .disabled(!state.canSave)
                }
            }
        } footer: {
            Text("Only people in your groups see your name and photo.")
        }
        .animation(.snappy, value: state.avatarColor)
    }

    private func nameHint(_ state: EditProfileUiState) -> LocalizedStringKey {
        switch state.nameError {
        case .tooShort: "Type at least \(Int(state.nameMinLength)) letters."
        case .tooLong: "Use up to \(Int(state.nameMaxLength)) characters."
        default: "From \(Int(state.nameMinLength)) to \(Int(state.nameMaxLength)) characters"
        }
    }
}

/// The avatar with "Edit photo": take one (iPhone and iPad), choose one, or remove it.
private struct PhotoEditor: View {
    let model: EditProfileModel

    @State private var pickedPhoto: PhotosPickerItem?
    @State private var showsPhotoPicker = false
    @State private var showsCamera = false

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            AvatarPhoto(
                initial: state.avatarInitial,
                color: state.avatarColor,
                photoUrl: state.photoUrl,
                size: 96,
            )
            .overlay {
                if state.isPreparingPhoto { ProgressView() }
            }
            .accessibilityHidden(true)
            Menu {
                #if os(iOS)
                if CameraPicker.isAvailable {
                    Button("Take photo", systemImage: "camera") { showsCamera = true }
                }
                #endif
                // Why: the system picker needs no photo library permission and returns one image, or nothing.
                Button("Choose photo", systemImage: "photo.on.rectangle") { showsPhotoPicker = true }
                if state.hasPhoto {
                    Button("Remove photo", systemImage: "trash", role: .destructive) {
                        model.send(EditProfileUiEventOnPhotoRemoved.shared)
                    }
                }
            } label: {
                Text("Edit photo")
            }
            .menuStyle(.borderlessButton)
            .fixedSize()
            if state.showsPhotoError {
                Text("Couldn't use this photo. Try another one.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .photosPicker(isPresented: $showsPhotoPicker, selection: $pickedPhoto, matching: .images)
        .onChange(of: pickedPhoto) { _, item in
            guard let item else { return }
            pickedPhoto = nil
            Task { model.photoPicked(try? await item.loadTransferable(type: Data.self)) }
        }
        #if os(iOS)
        .fullScreenCover(isPresented: $showsCamera) {
            CameraPicker { data in model.photoPicked(data) }
                .ignoresSafeArea()
        }
        #endif
    }
}
