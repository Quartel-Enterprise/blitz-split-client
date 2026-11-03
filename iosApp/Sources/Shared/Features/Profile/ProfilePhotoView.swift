import Observation
import Shared
import SwiftUI

@Observable
final class ProfilePhotoModel {
    private(set) var state: ProfilePhotoUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createProfilePhotoViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ProfilePhotoUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any ProfilePhotoUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    func photoPicked(_ data: Data?) {
        guard let data else {
            send(ProfilePhotoUiEventOnPhotoUnreadable.shared)
            return
        }
        send(ProfilePhotoUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
    }

    isolated deinit {
        holder.clear()
    }
}

/// The profile's photo big (`Route.ProfilePhoto`), as Bible Planner's viewer: the photo, or the initial on its color,
/// with the shortcuts that change it at once under it. On iPhone and iPad it covers the screen over the blurred
/// profile, closed by ✕ or a tap outside; on the Mac it is a sheet on Ajustes, closed by Close or Esc. A picked photo
/// opens "Adjust photo" over it.
struct ProfilePhotoView: View {
    @State private var model = ProfilePhotoModel()
    @State private var showsPhotoPicker = false
    @State private var takesPhoto = false
    @State private var isWide = false

    var body: some View {
        content
            .photoLibraryPicker(isPresented: $showsPhotoPicker) { data in model.photoPicked(data) }
            #if os(iOS)
            .cameraCapture(isRequested: $takesPhoto) { data in model.photoPicked(data) }
            #endif
            .photoCrop(model.state.photoCrop, frame: .circle) { model.send(ProfilePhotoUiEventOnPhotoCrop(event: $0)) }
    }

    #if os(iOS)
    private var content: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
                .contentShape(.rect)
                .onTapGesture(perform: dismiss)
                .accessibilityHidden(true)
            photo
        }
        .overlay(alignment: .topTrailing) {
            Button("Close", systemImage: "xmark", action: dismiss)
                .labelStyle(.iconOnly)
                .font(.system(size: 18, weight: .semibold))
                .frame(width: 44, height: 44)
                .glassEffect(.regular.interactive(), in: .circle)
                .padding(14)
        }
        .environment(\.colorScheme, .dark)
        .onGeometryChange(for: Bool.self, of: { $0.size.width >= 600 }) { isWide = $0 }
        .presentationBackground(.ultraThinMaterial)
    }
    #else
    private var content: some View {
        photo
            .padding(32)
            .frame(minWidth: 440)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: dismiss)
                }
            }
    }
    #endif

    @ViewBuilder
    private var photo: some View {
        let state = model.state
        // Only the saved profile is drawn, never "?" on the default color
        if state.isLoaded {
            VStack(spacing: 28) {
                let size: CGFloat = isWide ? 260 : 200
                AvatarPhoto(initial: state.avatarInitial, color: state.avatarColor, photoUrl: state.photoUrl, size: size)
                    .shadow(color: .black.opacity(0.35), radius: 20, y: 8)
                    .overlay {
                        if state.isPreparingPhoto {
                            Circle().fill(.black.opacity(0.4))
                            ProgressView().controlSize(.large)
                        }
                    }
                    .accessibilityLabel("Profile photo")
                if state.showsPhotoError {
                    Text("Couldn't use this photo. Try another one.")
                        .font(.footnote)
                        .foregroundStyle(Palette.negative)
                        .multilineTextAlignment(.center)
                }
                shortcuts(state)
            }
        }
    }

    private func shortcuts(_ state: ProfilePhotoUiState) -> some View {
        HStack(alignment: .top, spacing: 16) {
            if let provider = state.accountPhotoProvider {
                AccountPhotoShortcut(provider: provider) {
                    model.send(ProfilePhotoUiEventOnAccountPhotoSelected.shared)
                }
            }
            #if os(iOS)
            if CameraPicker.isAvailable {
                PhotoShortcut(title: Text("Camera"), systemImage: "camera", accessibilityLabel: "Take photo") {
                    takesPhoto = true
                }
            }
            #endif
            // The system picker needs no photo library permission and returns one image, or nothing
            PhotoShortcut(title: Text("Gallery"), systemImage: "photo.on.rectangle", accessibilityLabel: "Choose photo") {
                showsPhotoPicker = true
            }
            if state.hasPhoto {
                PhotoShortcut(
                    title: Text("Remove"),
                    systemImage: "trash",
                    accessibilityLabel: "Remove photo",
                    isDestructive: true,
                ) {
                    model.send(ProfilePhotoUiEventOnPhotoRemoved.shared)
                }
            }
        }
    }

    private func dismiss() {
        model.send(ProfilePhotoUiEventOnDismissed.shared)
    }
}

/// A round glass button with its title under it, as Bible Planner's viewer draws its shortcuts.
private struct PhotoShortcut: View {
    let title: Text
    let systemImage: String
    let accessibilityLabel: LocalizedStringKey
    var isDestructive = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 22, weight: .medium))
                    .frame(width: 58, height: 58)
                    .glassEffect(
                        isDestructive ? .regular.tint(Palette.negative.opacity(0.35)).interactive() : .regular.interactive(),
                        in: .circle,
                    )
                title
                    .font(.caption.weight(.medium))
            }
            .foregroundStyle(isDestructive ? Palette.negative : .primary)
            .frame(width: 76)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}

/// The account's own photo: its logo on white, as Bible Planner's viewer and the sign in buttons draw it.
private struct AccountPhotoShortcut: View {
    let provider: AccountProvider
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                logo
                    .frame(width: 58, height: 58)
                    .background(.white, in: .circle)
                Text(verbatim: provider.displayName)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.primary)
            }
            .frame(width: 76)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Use \(provider.displayName) account photo")
    }

    @ViewBuilder
    private var logo: some View {
        if provider == .google {
            Image(decorative: "GoogleLogo")
                .resizable()
                .frame(width: 24, height: 24)
        } else {
            Image(systemName: "apple.logo")
                .font(.system(size: 24))
                .foregroundStyle(.black)
        }
    }
}
