import SwiftUI

extension AnyTransition {
    /// Slides in from `edge` while fading in, as a toast or a notice comes in; with Reduce Motion on it only fades.
    static func slideIn(from edge: Edge, reduceMotion: Bool) -> AnyTransition {
        reduceMotion ? .opacity : .move(edge: edge).combined(with: .opacity)
    }
}

/// Plays `progress` from 0 to 1 each time `id` changes, as a chart grows or sweeps in. With Reduce Motion on it is
/// kept at 1, so turning the setting off later doesn't leave the chart empty; the view draws it whole meanwhile, from
/// its first frame, by reading the setting itself.
private struct PlaysIn<ID: Equatable>: ViewModifier {
    @Binding var progress: Double
    let id: ID
    let duration: Double

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.task(id: id) {
            if reduceMotion {
                progress = 1
                return
            }
            progress = 0
            try? await Task.sleep(for: .milliseconds(20))
            withAnimation(.easeOut(duration: duration)) { progress = 1 }
        }
    }
}

extension View {
    func playsIn<ID: Equatable>(_ progress: Binding<Double>, id: ID, duration: Double) -> some View {
        modifier(PlaysIn(progress: progress, id: id, duration: duration))
    }
}
