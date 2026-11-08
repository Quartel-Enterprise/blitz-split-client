import Observation
import Shared
import SwiftUI

@Observable
final class GroupPhotoModel {
    private(set) var state: GroupPhotoUiState

    @ObservationIgnored private let holder: ViewModelHolder<GroupPhotoViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteGroupPhoto) {
        holder = BlitzSplitApple.shared.createGroupPhotoViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupPhotoUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any GroupPhotoUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    func photoPicked(_ data: Data?) {
        guard let data else {
            send(GroupPhotoUiEventOnPhotoUnreadable.shared)
            return
        }
        send(GroupPhotoUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
    }

    isolated deinit {
        holder.clear()
    }
}

/// The group's photo big (`Route.GroupPhoto`), the profile's viewer for a group: the photo, or the initials on the
/// color, in the group's rounded square, with the shortcuts that change it at once under it (`PhotoViewer`). A picked
/// photo opens "Adjust photo" over it, framed in the same square.
struct GroupPhotoView: View {
    @State private var model: GroupPhotoModel
    @State private var showsPhotoPicker = false
    @State private var takesPhoto = false

    init(route: RouteGroupPhoto) {
        _model = State(initialValue: GroupPhotoModel(route: route))
    }

    var body: some View {
        PhotoViewer(dismiss: dismiss) { size in photo(size: size) }
            .photoViewerPickers(showsPhotoPicker: $showsPhotoPicker, takesPhoto: $takesPhoto) { model.photoPicked($0) }
            .photoCrop(model.state.photoCrop, frame: .roundedSquare) { model.send(GroupPhotoUiEventOnPhotoCrop(event: $0)) }
    }

    @ViewBuilder
    private func photo(size: CGFloat) -> some View {
        let state = model.state
        // Only the saved group is drawn, never "?" on the default color
        if state.isLoaded {
            VStack(spacing: 28) {
                GroupAvatar(initials: state.initials, color: state.color, photoUrl: state.photoUrl, size: size)
                    .shadow(color: .black.opacity(0.35), radius: 20, y: 8)
                    .overlay {
                        if state.isPreparingPhoto {
                            RoundedRectangle(cornerRadius: size * 0.32).fill(.black.opacity(0.4))
                            ProgressView().controlSize(.large)
                        }
                    }
                    .accessibilityLabel("Group photo")
                PhotoViewerStatus(showsError: state.showsPhotoError)
                PhotoViewerShortcuts(
                    hasPhoto: state.hasPhoto,
                    takesPhoto: $takesPhoto,
                    showsPhotoPicker: $showsPhotoPicker,
                    remove: { model.send(GroupPhotoUiEventOnPhotoRemoved.shared) },
                ) {}
            }
        }
    }

    private func dismiss() {
        model.send(GroupPhotoUiEventOnDismissed.shared)
    }
}
