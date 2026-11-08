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
/// with the shortcuts that change it at once under it (`PhotoViewer`). A picked photo opens "Adjust photo" over it.
struct ProfilePhotoView: View {
    @State private var model = ProfilePhotoModel()
    @State private var showsPhotoPicker = false
    @State private var takesPhoto = false

    var body: some View {
        PhotoViewer(dismiss: dismiss) { size in photo(size: size) }
            .photoViewerPickers(showsPhotoPicker: $showsPhotoPicker, takesPhoto: $takesPhoto) { model.photoPicked($0) }
            .photoCrop(model.state.photoCrop, frame: .circle) { model.send(ProfilePhotoUiEventOnPhotoCrop(event: $0)) }
    }

    @ViewBuilder
    private func photo(size: CGFloat) -> some View {
        let state = model.state
        // Only the saved profile is drawn, never "?" on the default color
        if state.isLoaded {
            VStack(spacing: 28) {
                AvatarPhoto(initial: state.avatarInitial, color: state.avatarColor, photoUrl: state.photoUrl, size: size)
                    .shadow(color: .black.opacity(0.35), radius: 20, y: 8)
                    .overlay {
                        if state.isPreparingPhoto {
                            Circle().fill(.black.opacity(0.4))
                            ProgressView().controlSize(.large)
                        }
                    }
                    .accessibilityLabel("Profile photo")
                PhotoViewerStatus(showsError: state.showsPhotoError)
                PhotoViewerShortcuts(
                    hasPhoto: state.hasPhoto,
                    takesPhoto: $takesPhoto,
                    showsPhotoPicker: $showsPhotoPicker,
                    remove: { model.send(ProfilePhotoUiEventOnPhotoRemoved.shared) },
                ) {
                    if let provider = state.accountPhotoProvider {
                        AccountPhotoShortcut(provider: provider) {
                            model.send(ProfilePhotoUiEventOnAccountPhotoSelected.shared)
                        }
                    }
                }
            }
        }
    }

    private func dismiss() {
        model.send(ProfilePhotoUiEventOnDismissed.shared)
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
