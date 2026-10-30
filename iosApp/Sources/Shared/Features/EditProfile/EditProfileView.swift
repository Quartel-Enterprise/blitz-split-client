import AVFoundation
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
        // Why: the color only shows behind the initial, so it is offered while there is no photo.
        if state.showsAvatarColors {
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
    @State private var showsCameraDenied = false

    #if os(iOS)
    /// Why: the system camera shows a black screen when access was refused, so ask first and explain a "no".
    private func takePhoto() async {
        let isAllowed = switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: true
        case .notDetermined: await AVCaptureDevice.requestAccess(for: .video)
        default: false
        }
        if isAllowed { showsCamera = true } else { showsCameraDenied = true }
    }
    #endif

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
            .overlay(alignment: .bottomTrailing) {
                Menu {
                    #if os(iOS)
                    if CameraPicker.isAvailable {
                        Button("Take photo", systemImage: "camera") { Task { await takePhoto() } }
                    }
                    #endif
                    // Why: the system picker needs no photo library permission and returns one image, or nothing.
                    Button("Choose photo", systemImage: "photo.on.rectangle") { showsPhotoPicker = true }
                    if state.showsUseAccountPhoto, let provider = state.accountProvider {
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
                    Image(systemName: "pencil")
                        .font(.system(size: 14, weight: .semibold))
                        .frame(width: 32, height: 32)
                        .glassEffect(.regular.interactive(), in: .circle)
                }
                .menuStyle(.borderlessButton)
                .menuIndicator(.hidden)
                .fixedSize()
                .offset(x: 4, y: 4)
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
        .alert("Camera access is off", isPresented: $showsCameraDenied) {
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Allow camera access in Settings to take a photo.")
        }
        #endif
    }
}
