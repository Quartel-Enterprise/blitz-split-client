import Shared
import SwiftUI

/// The watch's colors (canvas page 15): dark only, the Apple dark tokens on black, the money's green and orange and
/// the accent shared with the app and its widget (`ColorToken`).
enum WatchPalette {
    static let card = Color(rgb: 0x1C1C1E)
    static let secondary = Color(rgb: 0x98989F)
    static let accent = Color(ColorToken.accent)
    /// What reads on the accent: a prominent button's label, the check of "Payment recorded".
    static let onAccent = Color(rgb: 0x00344E)
    static let positive = Color(ColorToken.positive)
    static let negative = Color(ColorToken.negative)
    /// The dot of a change waiting to go up ("waiting to send").
    static let pending = Color(rgb: 0xD97706)

    /// The money's color, the widget's own rule: green for what comes back, orange for what you owe, secondary at zero.
    static func balance(_ minor: Int64) -> Color {
        WidgetPalette.balance(.init(minor: minor))
    }
}

private extension Color {
    /// The watch is dark only: one value per color.
    nonisolated init(rgb: UInt32) {
        self.init(light: rgb, dark: rgb)
    }
}

extension AvatarColor {
    /// The face's background and initials, the dark tones of the app's avatar colors (`AvatarColor.container`).
    var watchContainer: Color {
        switch self {
        case .peach: Color(rgb: 0x6B3B00)
        case .green: Color(rgb: 0x055315)
        case .lilac: Color(rgb: 0x5B3383)
        case .sand: Color(rgb: 0x504700)
        default: Color(rgb: 0x00458F)
        }
    }

    var watchContent: Color {
        switch self {
        case .peach: Color(rgb: 0xFFDCC0)
        case .green: Color(rgb: 0xAAF5A4)
        case .lilac: Color(rgb: 0xF0DBFF)
        case .sand: Color(rgb: 0xF6E468)
        default: Color(rgb: 0xD7E2FF)
        }
    }
}

extension View {
    /// A prominent button's label in the color made for the accent: the system's white doesn't read on the dark
    /// accent (check-prominent-labels.py).
    func prominentLabel() -> some View {
        foregroundStyle(WatchPalette.onAccent)
    }

    /// A card of the watch's lists (15.3, 15.4): the dark platter, rounded as the system's rows.
    func watchCard() -> some View {
        padding(.horizontal, 12)
            .padding(.vertical, 9)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(WatchPalette.card, in: .rect(cornerRadius: 22))
    }
}

/// A person's or a group's face: the photo, or the initials on its color.
struct WatchAvatar: View {
    let initials: String
    let color: AvatarColor?
    let photoUrl: String?
    var size: CGFloat = 24

    var body: some View {
        Group {
            if let photoUrl, let url = URL(string: photoUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    initialsView
                }
            } else {
                initialsView
            }
        }
        .frame(width: size, height: size)
        .clipShape(.circle)
        .accessibilityHidden(true)
    }

    private var initialsView: some View {
        let color = color ?? .blue
        return Text(verbatim: initials)
            .font(.system(size: size * 0.42, weight: .bold))
            .foregroundStyle(color.watchContent)
            .frame(width: size, height: size)
            .background(color.watchContainer)
    }
}
