import Shared
import SwiftUI

/// "No connection." over the welcome actions: a Liquid Glass notice, as the canvas draws it.
struct GlassNotice: View {
    let title: LocalizedStringKey
    let message: LocalizedStringKey

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "wifi.slash")
                .foregroundStyle(.secondary)
            Text("\(Text(title).fontWeight(.semibold)) \(Text(message))")
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassEffect(.regular, in: .rect(cornerRadius: 22))
        .accessibilityElement(children: .combine)
    }
}

/// Where the missing connection blocks the main action (recovering), it is a card with a title.
struct ConnectionCard: View {
    let title: LocalizedStringKey
    let message: LocalizedStringKey

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "wifi.slash")
                .foregroundStyle(Palette.negative)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(message).font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 22))
        .accessibilityElement(children: .combine)
    }
}

/// The uppercase caption above a grouped card ("AVATAR COLOR", "YOUR NAME").
struct SectionCaption: View {
    let text: LocalizedStringKey

    var body: some View {
        Text(text)
            .font(.footnote)
            .textCase(.uppercase)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The rounded card of a grouped screen.
struct GroupedCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.card, in: .rect(cornerRadius: 26))
    }
}

extension View {
    /// A finger's 44 pt around a small control on iPhone and iPad (Apple's minimum), on a button's label: the hit
    /// area grows to 44 pt while the layout keeps what the overlaps give back, so nothing around it moves. The
    /// Mac's pointer needs none.
    func fingerTarget(horizontalOverlap: CGFloat = 0, verticalOverlap: CGFloat = 0) -> some View {
        #if os(iOS)
        frame(minWidth: 44, minHeight: 44)
            .contentShape(.rect)
            .padding(.horizontal, -horizontalOverlap)
            .padding(.vertical, -verticalOverlap)
        #else
        self
        #endif
    }
}

/// The "or" between the account buttons and "Use without an account".
struct OrDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle().frame(height: 0.5).foregroundStyle(.separator)
            Text("or").font(.footnote).foregroundStyle(.secondary)
            Rectangle().frame(height: 0.5).foregroundStyle(.separator)
        }
        .accessibilityHidden(true)
    }
}

/// The main action of a screen: a prominent Liquid Glass capsule.
struct PrimaryButton: View {
    let title: LocalizedStringKey
    var systemImage: String?
    var isEnabled = true
    var isLoading = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if isLoading {
                    ProgressView()
                } else if let systemImage {
                    Label(title, systemImage: systemImage)
                } else {
                    Text(title)
                }
            }
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 36)
        }
        .buttonStyle(.glassProminent)
        .prominentLabel()
        .buttonBorderShape(.capsule)
        .controlSize(.large)
        .disabled(!isEnabled)
    }
}

/// A secondary action tinted with the accent ("Use without an account", "Join with a link or QR code").
struct TintedButton: View {
    let title: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 36)
        }
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        .controlSize(.large)
    }
}

/// The tile with the app's logo that heads the welcome pitch.
struct LogoBadge: View {
    var size: CGFloat = 72

    var body: some View {
        Image(decorative: "Logo")
            .resizable()
            .scaledToFit()
            .foregroundStyle(Palette.onAccent)
            .padding(size * 0.2)
            .frame(width: size, height: size)
            .background(Palette.accent, in: .rect(cornerRadius: size / 4))
            .glassEffect(.regular.tint(Palette.accent), in: .rect(cornerRadius: size / 4))
            .shadow(color: Palette.accent.opacity(0.35), radius: 15, y: 12)
            .accessibilityHidden(true)
    }
}

/// The initial stays under the photo, so it shows while the photo loads or when it fails to. A picked
/// photo arrives as a data URI, which AsyncImage loads like any other URL. Without a font, the initial
/// scales with the avatar.
struct AvatarPhoto: View {
    let initial: String
    let color: AvatarColor
    let photoUrl: String?
    let size: CGFloat
    var font: Font?

    var body: some View {
        Text(initial)
            .font(font ?? .system(size: size * 0.42, weight: .bold))
            .foregroundStyle(color.content)
            .frame(width: size, height: size)
            .background(color.container, in: .circle)
            .overlay {
                if let photoUrl, let url = URL(string: photoUrl) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image.renderingMode(.original).resizable().scaledToFill()
                        }
                    }
                    .frame(width: size, height: size)
                    .clipShape(.circle)
                }
            }
    }
}

/// The amber dot of what is saved here and not on the server yet, the one every screen draws. Hidden from VoiceOver
/// beside words that already say it; a dot alone takes its own `label`.
struct PendingDot: View {
    var size: CGFloat = 8
    var color: Color = Palette.pending
    var label: Text?

    var body: some View {
        let dot = Circle()
            .fill(color)
            .frame(width: size, height: size)
        if let label {
            dot.accessibilityLabel(label)
        } else {
            dot.accessibilityHidden(true)
        }
    }
}
