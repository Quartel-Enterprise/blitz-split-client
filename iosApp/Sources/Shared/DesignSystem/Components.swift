import SwiftUI

/// "Sem conexão." over the welcome actions: a Liquid Glass notice, as the canvas draws it.
struct GlassNotice: View {
    let title: String
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "wifi.slash")
                .foregroundStyle(.secondary)
            Text("\(Text(title).fontWeight(.semibold)) \(message)")
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
    let title: String
    let message: String

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

/// The uppercase caption above a grouped card ("COR DO AVATAR", "SEU NOME").
struct SectionCaption: View {
    let text: String

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

/// The "or" between the account buttons and "Usar sem conta".
struct OrDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle().frame(height: 0.5).foregroundStyle(.separator)
            Text("ou").font(.footnote).foregroundStyle(.secondary)
            Rectangle().frame(height: 0.5).foregroundStyle(.separator)
        }
        .accessibilityHidden(true)
    }
}

/// The main action of a screen: a prominent Liquid Glass capsule.
struct PrimaryButton: View {
    let title: String
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
        .buttonBorderShape(.capsule)
        .controlSize(.large)
        .disabled(!isEnabled)
    }
}

/// A secondary action tinted with the accent ("Usar sem conta", "Entrar com link ou QR").
struct TintedButton: View {
    let title: String
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

/// The "R$" tile that heads the welcome pitch.
struct CurrencyBadge: View {
    var size: CGFloat = 72

    var body: some View {
        Text("R$")
            .font(.system(size: size * 0.39, weight: .heavy))
            .foregroundStyle(Palette.onAccent)
            .frame(width: size, height: size)
            .background(Palette.accent, in: .rect(cornerRadius: size / 4))
            .glassEffect(.regular.tint(Palette.accent), in: .rect(cornerRadius: size / 4))
            .shadow(color: Palette.accent.opacity(0.35), radius: 15, y: 12)
            .accessibilityHidden(true)
    }
}
