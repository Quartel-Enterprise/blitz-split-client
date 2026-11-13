import Shared
import SwiftUI

/// Editar grupo's icon: the avatar and what changes it. iOS opens "Group icon" (6.4d) from the avatar or "Change icon
/// or photo" (the pencil on its corner): the color picked there reaches the form on ✓, and "Choose photo" or "Take photo" pick a photo once the
/// sheet is gone, so the picker never opens over a sheet closing. The Mac picks the color in the form itself, and
/// "Choose Photo…" a file, as its board draws it.
struct EditGroupIconSection: View {
    let model: EditGroupModel
    let usesCompactIcon: Bool

    @State private var showsPhotoPicker = false
    #if os(iOS)
    @State private var takesPhoto = false
    /// What the sheet asked for, opened once it is dismissed.
    @State private var pendingSource: PhotoSource?
    #else
    @State private var importsFile = false
    #endif

    private var state: EditGroupUiState {
        model.state
    }

    var body: some View {
        let layout = usesCompactIcon
            ? AnyLayout(HStackLayout(spacing: 12))
            : AnyLayout(VStackLayout(spacing: 8))
        layout {
            avatar
            #if os(macOS)
            HStack {
                ForEach(state.colors, id: \.self) { color in
                    ColorSwatch(color: color, isSelected: color == state.selectedColor) {
                        model.send(EditGroupUiEventOnColorSelected(color: color))
                    }
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("Icon color")
            Button("Choose Photo…") { importsFile = true }
            #endif
            if state.showsPhotoError {
                Text("Couldn't use this photo. Try another one.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .photoLibraryPicker(isPresented: $showsPhotoPicker) { data in model.photoPicked(data) }
        #if os(iOS)
        .cameraCapture(isRequested: $takesPhoto) { data in model.photoPicked(data) }
        .sheet(isPresented: iconEditorBinding, onDismiss: openPendingSource) {
            if let editor = state.iconEditor {
                GroupIconSheet(
                    model: model,
                    editor: editor,
                    initials: state.initials,
                    onPick: { source in
                        pendingSource = source
                        model.send(EditGroupUiEventOnIconDismissed.shared)
                    },
                )
            }
        }
        #else
        .fileImporter(isPresented: $importsFile, allowedContentTypes: [.image]) { result in
            // A cancelled panel picked nothing, so it says nothing either
            if case let .failure(error) = result, (error as? CocoaError)?.code == .userCancelled { return }
            model.photoPicked(readImportedPhoto(result))
        }
        #endif
    }

    private var avatar: some View {
        GroupAvatar(
            initials: state.initials,
            color: state.color,
            photoUrl: state.photoUrl,
            size: usesCompactIcon ? 56 : 96,
        )
        .overlay {
            if state.isPreparingPhoto { ProgressView() }
        }
        #if os(iOS)
        .onTapGesture { model.send(EditGroupUiEventOnIconClicked.shared) }
        // Why: VoiceOver reaches "Group icon" through the pencil alone, not the initials too.
        .accessibilityHidden(true)
        .overlay(alignment: .bottomTrailing) {
            // Why: the pencil on the corner opens "Group icon", as the profile's photo does.
            Button {
                model.send(EditGroupUiEventOnIconClicked.shared)
            } label: {
                CornerGlassIcon(systemName: "pencil")
            }
            .buttonStyle(.plain)
            .offset(x: 4 + CornerGlassIcon.targetPadding, y: 4 + CornerGlassIcon.targetPadding)
            .accessibilityLabel("Change icon or photo")
        }
        #else
        .accessibilityHidden(true)
        #endif
    }

    #if os(iOS)
    private var iconEditorBinding: Binding<Bool> {
        Binding(
            get: { model.state.iconEditor != nil },
            set: { if !$0 { model.send(EditGroupUiEventOnIconDismissed.shared) } },
        )
    }

    private func openPendingSource() {
        switch pendingSource {
        case .library: showsPhotoPicker = true
        case .camera: takesPhoto = true
        case nil: break
        }
        pendingSource = nil
    }
    #endif
}

#if os(macOS)
/// The file "Choose Photo…" picked on the Mac, read inside its security scope; nil when it couldn't be.
func readImportedPhoto(_ result: Result<URL, any Error>) -> Data? {
    guard case let .success(url) = result else { return nil }
    let isScoped = url.startAccessingSecurityScopedResource()
    defer { if isScoped { url.stopAccessingSecurityScopedResource() } }
    return try? Data(contentsOf: url)
}
#endif

#if os(iOS)
enum PhotoSource {
    case library
    case camera
}

/// "Group icon": a medium sheet with ✕, the colors as tiles drawing the initials, and the photo's sources. Only Android
/// picks the shape.
private struct GroupIconSheet: View {
    let model: EditGroupModel
    let editor: GroupIconEditor
    let initials: String
    let onPick: (PhotoSource) -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        ForEach(editor.colors, id: \.self) { color in
                            colorTile(color)
                            if color != editor.colors.last { Spacer(minLength: 6) }
                        }
                    }
                    .accessibilityElement(children: .contain)
                    .accessibilityLabel("Color")
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
                Section {
                    Button("Choose photo", systemImage: "photo.on.rectangle") { onPick(.library) }
                    if CameraPicker.isAvailable {
                        Button("Take photo", systemImage: "camera") { onPick(.camera) }
                    }
                }
            }
            .navigationTitle("Group icon")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model.send(EditGroupUiEventOnIconDismissed.shared)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Use this icon", systemImage: "checkmark") {
                        model.send(EditGroupUiEventOnIconConfirmed.shared)
                    }
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                }
            }
        }
        .presentationDetents([.medium])
        // An iPhone Duo open: in the half before the fold, never over it
        .foldSheetPlacement()
    }

    private func colorTile(_ color: AvatarColor) -> some View {
        let isSelected = color == editor.color
        return Button {
            model.send(EditGroupUiEventOnIconColorPicked(color: color))
        } label: {
            Text(verbatim: initials)
                .font(.system(size: 17, weight: .heavy))
                .foregroundStyle(color.content)
                .frame(width: 52, height: 52)
                .background(color.container, in: .rect(cornerRadius: 15))
                .overlay {
                    RoundedRectangle(cornerRadius: 15)
                        .strokeBorder(Palette.accent, lineWidth: isSelected ? 3 : 0)
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(color.label)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}
#endif
