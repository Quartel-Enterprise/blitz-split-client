import SwiftUI

extension View {
    /// A `ShareLink` opens the share sheet itself and takes no action of its own, so its tap reaches the shared code
    /// beside it, for the event the same share sends on Android and the web.
    func onShareTap(_ action: @escaping () -> Void) -> some View {
        simultaneousGesture(TapGesture().onEnded(action))
    }
}
