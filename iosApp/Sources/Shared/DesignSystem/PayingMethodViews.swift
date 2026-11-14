import Shared
import SwiftUI

/// What the person calls a payment method, or its type's name when they gave it none.
extension PayingMethod {
    var label: String {
        name.trimmingCharacters(in: .whitespaces).isEmpty ? type.label : name
    }

    /// The type under a named method ("Nubank" over "Credit card"); nil when the label already is the type.
    var subtitle: String? {
        name.trimmingCharacters(in: .whitespaces).isEmpty ? nil : type.label
    }
}

extension PayingMethodType {
    /// The type's name, in the export too. Pix and Bitcoin are the same word in every language.
    var label: String {
        switch self {
        case .creditCard: String(localized: "Credit card", bundle: .appLanguage)
        case .debitCard: String(localized: "Debit card", bundle: .appLanguage)
        case .cash: String(localized: "Cash", bundle: .appLanguage)
        case .pix: "Pix"
        case .bankTransfer: String(localized: "Bank transfer", bundle: .appLanguage)
        case .bitcoin: "Bitcoin"
        default: String(
            localized: "Other (a payment method type)",
            defaultValue: "Other",
            bundle: .appLanguage,
            comment: "A payment method type",
        )
        }
    }

    fileprivate var symbol: String {
        switch self {
        case .creditCard, .debitCard: "creditcard.fill"
        case .cash: "banknote"
        case .bankTransfer: "building.columns"
        case .bitcoin: "bitcoinsign"
        default: "ellipsis"
        }
    }
}

extension CardColor {
    /// The card's swatch, the same in light and dark, so white reads on each.
    var color: Color {
        let hex: UInt32 = switch self {
        case .purple: 0x6D4AB8
        case .blue: 0x1565C0
        case .green: 0x2E7D32
        case .orange: 0xE65100
        case .red: 0xC62828
        case .pink: 0xAD1457
        case .gold: 0x9A7500
        default: 0x455A64
        }
        return Color(light: hex, dark: hex)
    }

    /// The swatch's name, which VoiceOver reads.
    var label: String {
        switch self {
        case .purple: String(localized: "Purple (a card's color)", defaultValue: "Purple", bundle: .appLanguage)
        case .blue: String(localized: "Blue (a card's color)", defaultValue: "Blue", bundle: .appLanguage)
        case .green: String(localized: "Green (a card's color)", defaultValue: "Green", bundle: .appLanguage)
        case .orange: String(localized: "Orange (a card's color)", defaultValue: "Orange", bundle: .appLanguage)
        case .red: String(localized: "Red (a card's color)", defaultValue: "Red", bundle: .appLanguage)
        case .pink: String(localized: "Pink (a card's color)", defaultValue: "Pink", bundle: .appLanguage)
        case .gold: String(localized: "Gold (a card's color)", defaultValue: "Gold", bundle: .appLanguage)
        default: String(localized: "Graphite (a card's color)", defaultValue: "Graphite", bundle: .appLanguage)
        }
    }
}

/// Pix's own mark (`PixKey` in the asset catalog, Material's Pix glyph, as Android draws it), scaling with Dynamic
/// Type as the symbols beside it do.
struct PixMark: View {
    @ScaledMetric(relativeTo: .subheadline) private var size = 18.0

    var body: some View {
        Image(decorative: "PixKey").resizable().scaledToFit().frame(width: size, height: size)
    }
}

/// A payment method's tile, brand neutral: a card filled with its color under a white card, Pix's and Bitcoin's
/// tiles as the keys draw them, and a neutral tile for cash, a bank transfer and anything else.
struct PayingMethodTile: View {
    let type: PayingMethodType
    /// A card's swatch; nil for anything but a card.
    let cardColor: CardColor?
    var size: CGFloat = 30

    init(type: PayingMethodType, cardColor: CardColor?, size: CGFloat = 30) {
        self.type = type
        self.cardColor = cardColor
        self.size = size
    }

    init(method: PayingMethod, size: CGFloat = 30) {
        self.init(type: method.type, cardColor: method.cardColor, size: size)
    }

    var body: some View {
        let colors = tileColors
        glyph
            .font(size < 26 ? .caption.weight(.semibold) : .subheadline.weight(.semibold))
            .foregroundStyle(colors.content)
            .frame(width: size, height: size)
            .background(colors.tile, in: .rect(cornerRadius: size * 0.27))
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var glyph: some View {
        if type == .pix {
            PixMark()
        } else {
            Image(systemName: type.symbol)
        }
    }

    private var tileColors: (tile: AnyShapeStyle, content: AnyShapeStyle) {
        if type.isCard {
            let swatch = (cardColor ?? .graphite).color
            return (AnyShapeStyle(swatch), AnyShapeStyle(Color.white))
        }
        return switch type {
        case .pix: (AnyShapeStyle(Palette.pixTile), AnyShapeStyle(Palette.onPixTile))
        case .bitcoin: (AnyShapeStyle(Palette.bitcoinTile), AnyShapeStyle(Palette.onBitcoinTile))
        default: (AnyShapeStyle(.fill.secondary), AnyShapeStyle(.primary))
        }
    }
}

/// A method's tile beside its label, with its type under a named one.
struct PayingMethodLabel: View {
    let method: PayingMethod
    var tileSize: CGFloat = 30

    var body: some View {
        HStack(spacing: 12) {
            PayingMethodTile(method: method, size: tileSize)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: method.label)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                if let subtitle = method.subtitle {
                    Text(verbatim: subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
    }
}

/// "Default" beside a key or a payment method in the profile's lists.
struct DefaultBadge: View {
    let title: Text

    var body: some View {
        title
            .font(.footnote.weight(.semibold))
            .foregroundStyle(Palette.accent)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(Palette.accent.opacity(0.14), in: .rect(cornerRadius: 8))
    }
}

/// "Paid with" a method's tile and label, the bill's own mark of this device's user, with "Only you see this."
/// under it.
struct PaidWithLine: View {
    let method: PayingMethod
    var tileSize: CGFloat = 20

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) {
                Text("Paid with")
                PayingMethodTile(method: method, size: tileSize)
                Text(verbatim: method.label)
                    .lineLimit(1)
            }
            .accessibilityElement(children: .combine)
            Text("Only you see this.")
                .font(.footnote)
        }
    }
}

/// The picked method of a menu (Split a bill's "Paid with"): its tile and label, or "None".
struct PaidWithChoice: View {
    let method: PayingMethod?

    var body: some View {
        if let method {
            HStack(spacing: 8) {
                PayingMethodTile(method: method, size: 24)
                Text(verbatim: method.label)
                    .lineLimit(1)
            }
        } else {
            Text(LocalizedStringResource("None (no payment method)", defaultValue: "None"))
        }
    }
}

/// "None" and then each method, the menu "Paid with" opens; a method's icon is its type's glyph, since a menu draws
/// its items' icons in one color.
struct PaidWithMenuItems: View {
    let methods: [PayingMethod]
    let onPick: (String?) -> Void

    var body: some View {
        Button {
            onPick(nil)
        } label: {
            Text(LocalizedStringResource("None (no payment method)", defaultValue: "None"))
        }
        Divider()
        ForEach(methods, id: \.id) { method in
            Button {
                onPick(method.id)
            } label: {
                Label {
                    Text(verbatim: method.label)
                    if let subtitle = method.subtitle {
                        Text(verbatim: subtitle)
                    }
                } icon: {
                    if method.type == .pix {
                        Image(decorative: "PixKey")
                    } else {
                        Image(systemName: method.type.symbol)
                    }
                }
            }
        }
    }
}
