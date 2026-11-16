import Charts
import SwiftUI

/// One slice of a ring: who or what it is, its rounded share (a ring's adding up to 100) and its data tone.
struct StatsRingSlice {
    let id: String
    let percent: Double
    let color: Color
}

/// The ring of "By category" and "By person": slices with a thin gap between them, sweeping in when `ringKey`
/// changes (whole at once with Reduce Motion on). The picked slice pops out and the others fade; the pointer picks by
/// hovering, and a tap or a click lands on `onTap`, told whether a pointer made it. Shares that are all 0 leave a bare neutral ring. The center is the
/// caller's, as is the label VoiceOver reads for the whole ring.
struct StatsRing<Center: View>: View {
    let slices: [StatsRingSlice]
    let size: CGFloat
    let ringKey: String
    let label: Text
    @Binding var selected: Int?
    /// The slice tapped, and whether a pointer (the Mac's, an iPad's trackpad) was over the ring when it was.
    let onTap: (Int, Bool) -> Void
    @ViewBuilder let center: Center

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// 0 to 1 as the ring sweeps in, clockwise from the top.
    @State private var sweep: Double = 0
    /// A pointer is over the ring, so a click lands on a slice its hover already picked.
    @State private var isHovered = false

    private var isEmpty: Bool {
        !slices.contains { $0.percent > 0 }
    }

    var body: some View {
        let swept = reduceMotion ? 1 : sweep
        let hasGaps = slices.filter { $0.percent > 0 }.count > 1
        Chart {
            if isEmpty {
                SectorMark(angle: .value("Share", 100), innerRadius: .ratio(0.62), outerRadius: .ratio(0.92))
                    .foregroundStyle(Color.secondary.opacity(0.18))
            } else {
                ForEach(Array(slices.enumerated()), id: \.element.id) { index, slice in
                    SectorMark(
                        angle: .value("Share", slice.percent * swept),
                        innerRadius: .ratio(0.62),
                        outerRadius: .ratio(selected == index ? 1 : 0.92),
                        angularInset: hasGaps ? 1.5 : 0,
                    )
                    .foregroundStyle(slice.color)
                    .opacity(selected == nil || selected == index ? 1 : 0.4)
                }
                // The rest of the turn while the ring sweeps in, drawn in no color
                SectorMark(angle: .value("Share", 100 * (1 - swept)), innerRadius: .ratio(0.62), outerRadius: .ratio(0.92))
                    .foregroundStyle(.clear)
            }
        }
        .chartLegend(.hidden)
        .chartOverlay { _ in
            GeometryReader { geometry in
                Rectangle()
                    .fill(.clear)
                    .contentShape(Rectangle())
                    .onTapGesture { location in
                        if let index = slice(at: location, in: geometry.size) {
                            onTap(index, isHovered)
                        }
                    }
                    .onContinuousHover { phase in
                        switch phase {
                        case let .active(location):
                            isHovered = true
                            selected = slice(at: location, in: geometry.size)
                        case .ended:
                            isHovered = false
                            selected = nil
                        }
                    }
            }
        }
        .frame(width: size, height: size)
        .overlay {
            center
                .padding(.horizontal, 24)
                .allowsHitTesting(false)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        // The picked slice pops out at once with Reduce Motion, as the web's does
        .animation(reduceMotion ? nil : .smooth(duration: 0.25), value: selected)
        .playsIn($sweep, id: ringKey, duration: 0.6)
    }

    /// The slice under a point of the ring, by its angle clockwise from the top; nil deep in the hole or outside it.
    private func slice(at point: CGPoint, in size: CGSize) -> Int? {
        let radius = min(size.width, size.height) / 2
        let dx = point.x - size.width / 2
        let dy = point.y - size.height / 2
        let distance = (dx * dx + dy * dy).squareRoot()
        guard distance <= radius, distance >= radius * 0.4 else { return nil }
        var degrees = atan2(dy, dx) * 180 / .pi + 90
        if degrees < 0 { degrees += 360 }
        let total = slices.reduce(0) { $0 + $1.percent }
        guard total > 0 else { return nil }
        var end = 0.0
        for (index, slice) in slices.enumerated() {
            end += slice.percent / total * 360
            if slice.percent > 0, degrees < end { return index }
        }
        return slices.lastIndex { $0.percent > 0 }
    }
}

/// The ring's middle: the picked or largest share over its name.
struct StatsRingCenter: View {
    let percent: Int32
    let name: String

    var body: some View {
        VStack(spacing: 0) {
            Text(verbatim: "\(percent)%")
                .font(.headline.weight(.bold))
                .monospacedDigit()
                .contentTransition(.numericText())
            Text(verbatim: name)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }
}
