import Observation
import Shared
import SwiftUI

/// "Carla deleted the group “Ana's birthday”." once someone else deleted a group, on Groups and on Home. The notice is
/// state, not an action, so it waits until the screen is on top; the next deleted group follows once this one is gone.
@Observable
final class DeletedGroupToast {
    private(set) var count = 0
    /// Kept while the toast shows.
    private(set) var text: LocalizedStringKey = ""
    /// The toast's own duration, longer than the other toasts'.
    static let duration: Duration = .seconds(4)

    /// The notice on screen, so a state update meanwhile doesn't show it twice.
    @ObservationIgnored private var shownGroupId: String?

    /// Shows the notice once; `onShown` tells the ViewModel once the toast is gone, as Android's snackbar does, so the
    /// next deleted group doesn't replace it.
    func show(_ notice: DeletedGroupNotice?, onShown: @escaping () -> Void) {
        guard let notice, notice.groupId != shownGroupId else { return }
        shownGroupId = notice.groupId
        if let name = notice.deletedByName {
            text = "\(name) deleted the group “\(notice.groupName)”."
        } else {
            text = "The group “\(notice.groupName)” was deleted."
        }
        count += 1
        Task {
            try? await Task.sleep(for: Self.duration)
            onShown()
        }
    }
}
