import SwiftUI

/// The one loading placeholder of the Apple app (Pierre's call: nothing is guessed while loading). A section with no
/// answer yet draws these blocks in the shape of what comes, wrapped in `.shimmering()`, never an empty state, a zero
/// or a default.
///
/// ```swift
/// ShimmerList(count: 3).shimmering()
/// ```
extension View {
    /// Sweeps a soft highlight over the placeholder blocks inside, left to right, and makes them one "Loading"
    /// element for VoiceOver. With Reduce Motion the blocks stay still. A read of the device shows them only after
    /// `delay` (150 ms), fading in, so a fast read never flashes; pass nil where the wait is known to be long.
    /// `label` names what is loading for VoiceOver where "Loading" alone says too little.
    func shimmering(appearsAfter delay: Duration? = .milliseconds(150), label: Text = Text("Loading")) -> some View {
        modifier(ShimmerModifier(delay: delay, label: label))
    }
}

private struct ShimmerModifier: ViewModifier {
    let delay: Duration?
    let label: Text

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isShown = false

    func body(content: Content) -> some View {
        sweeping(content)
            .opacity(isShown || delay == nil ? 1 : 0)
            .task {
                guard let delay, !isShown else { return }
                try? await Task.sleep(for: delay)
                withAnimation(.easeIn(duration: 0.2)) { isShown = true }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
    }

    @ViewBuilder
    private func sweeping(_ content: Content) -> some View {
        if reduceMotion {
            content
        } else {
            TimelineView(.animation) { context in
                content.mask(Self.band(at: Self.phase(of: context.date)))
            }
        }
    }

    private static let period = 1.4

    /// 0 to 1 over each period, the same for every shimmer on screen, so they sweep together.
    private static func phase(of date: Date) -> Double {
        date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period) / period
    }

    /// The blocks show dimmed except under the band, which runs from past the leading edge to past the trailing one.
    private static func band(at phase: Double) -> LinearGradient {
        LinearGradient(
            stops: [
                .init(color: .black.opacity(0.55), location: 0.3),
                .init(color: .black, location: 0.5),
                .init(color: .black.opacity(0.55), location: 0.7),
            ],
            startPoint: UnitPoint(x: -1 + 2 * phase, y: 0.4),
            endPoint: UnitPoint(x: 2 * phase, y: 0.6),
        )
    }
}

// MARK: - Blocks

/// A line of text still coming, `width` wide (the whole width when nil).
struct ShimmerLine: View {
    var width: CGFloat?
    var height: CGFloat = 12

    var body: some View {
        Capsule()
            .fill(.fill.tertiary)
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: .leading)
    }
}

/// An avatar or a face still coming.
struct ShimmerCircle: View {
    var size: CGFloat = 44

    var body: some View {
        Circle()
            .fill(.fill.tertiary)
            .frame(width: size, height: size)
    }
}

/// A rounded block: a button, a field, a chart, a group's badge.
struct ShimmerBlock: View {
    var width: CGFloat?
    var height: CGFloat
    var cornerRadius: CGFloat = 12

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(.fill.tertiary)
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil)
    }
}

/// A list row still coming: a face (or a group's badge), a title over a subtitle, and an amount at the end.
struct ShimmerRow: View {
    enum Leading {
        case circle
        case badge
        case none
    }

    var leading: Leading = .circle
    var leadingSize: CGFloat = 44
    var showsTrailing = true

    var body: some View {
        HStack(spacing: 14) {
            switch leading {
            case .circle:
                ShimmerCircle(size: leadingSize)
            case .badge:
                ShimmerBlock(width: leadingSize, height: leadingSize, cornerRadius: leadingSize * 0.32)
            case .none:
                EmptyView()
            }
            VStack(alignment: .leading, spacing: 8) {
                ShimmerLine(width: 140, height: 12)
                ShimmerLine(width: 96, height: 10)
            }
            Spacer(minLength: 0)
            if showsTrailing {
                ShimmerLine(width: 64, height: 12)
            }
        }
        .padding(.vertical, 6)
    }
}

/// `count` rows still coming, one under the other.
struct ShimmerList: View {
    var count = 3
    var leading: ShimmerRow.Leading = .circle
    var showsTrailing = true

    var body: some View {
        VStack(spacing: 8) {
            ForEach(0..<count, id: \.self) { _ in
                ShimmerRow(leading: leading, showsTrailing: showsTrailing)
            }
        }
    }
}

/// What a card still coming holds, as Saldo geral or Seu saldo neste grupo: a caption, a big amount and its buttons.
/// The card's own background goes outside `.shimmering()`, so only the blocks sweep:
/// `ShimmerCard().shimmering().background(Palette.card, in: .rect(cornerRadius: 28))`.
struct ShimmerCard: View {
    var showsButtons = true

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ShimmerLine(width: 110, height: 12)
            ShimmerLine(width: 180, height: 28)
            if showsButtons {
                HStack(spacing: 12) {
                    ShimmerBlock(height: 40, cornerRadius: 20)
                    ShimmerBlock(height: 40, cornerRadius: 20)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// A form still coming: `fieldCount` fields, each a caption over a field.
struct ShimmerForm: View {
    var fieldCount = 4

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ForEach(0..<fieldCount, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 8) {
                    ShimmerLine(width: 90, height: 10)
                    ShimmerBlock(height: 44)
                }
            }
        }
        .padding(20)
    }
}

/// A whole screen still coming, before its first read: the blocks in the shape of its content, sweeping, in a column
/// as wide as the screen's own (620 pt at most), with the note under them while the account's copy waits offline.
/// The screen keeps its bars and title around it.
struct ShimmerScreen<Content: View>: View {
    var isWaitingForInternet = false
    var isAboutGroups = false
    @ViewBuilder let content: Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 16) {
                    content
                }
                .shimmering()
                if isWaitingForInternet {
                    WaitingForInternetNote(isAboutGroups: isAboutGroups)
                }
            }
            .padding(16)
            .frame(maxWidth: 620)
            .frame(maxWidth: .infinity)
        }
        .scrollDisabled(true)
    }
}

/// "No internet. Your data shows up once you're back online." under a shimmer while the account's first full sync
/// waits for the internet; Início and Grupos say "groups" instead of "data".
struct WaitingForInternetNote: View {
    var isAboutGroups = false

    var body: some View {
        Label {
            Text(isAboutGroups
                ? "No internet. Your groups show up once you're back online."
                : "No internet. Your data shows up once you're back online.")
        } icon: {
            Image(systemName: "wifi.slash")
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview("Blocks") {
    VStack(alignment: .leading, spacing: 24) {
        HStack(spacing: 12) {
            ShimmerCircle()
            ShimmerLine(width: 120)
        }
        ShimmerCard()
        ShimmerList(count: 3)
        ShimmerList(count: 2, leading: .badge)
    }
    .shimmering(appearsAfter: nil)
    .padding()
}

#Preview("Form") {
    ShimmerForm().shimmering(appearsAfter: nil)
}

#Preview("Screen waiting for the internet") {
    ShimmerScreen(isWaitingForInternet: true, isAboutGroups: true) {
        ShimmerCard()
            .background(Palette.card, in: .rect(cornerRadius: 28))
        ShimmerList(count: 4, leading: .badge)
    }
}
