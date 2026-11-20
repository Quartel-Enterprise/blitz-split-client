import SwiftUI
import WidgetKit

/// The balance widget's faces (board 15.8): the Home Screen's small and medium, the Lock Screen's circle and rectangle.
/// The watch's complication and Smart Stack card draw the same accessory and small faces later, so nothing here
/// reads the app's code: only `WidgetBalances` and the system.
struct BalanceWidgetView: View {
    let balances: WidgetBalances

    @Environment(\.widgetFamily) private var family

    var body: some View {
        content
            .appLanguage(balances.language)
            .widgetURL(WidgetBalancesStore.balancesURL)
    }

    @ViewBuilder
    private var content: some View {
        switch family {
        case .accessoryCircular: BalanceCircularView(balances: balances)
        case .accessoryRectangular: BalanceRectangularView(balances: balances)
        case .systemMedium: BalanceMediumView(balances: balances)
        default: BalanceSmallView(balances: balances)
        }
    }
}

/// The overall balance: "Balances", "+R$ 186,40", "to get back", then whom you're owed and whom you owe.
struct BalanceSmallView: View {
    let balances: WidgetBalances

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BalanceWidgetHeader()
            OverallBalanceView(balances: balances, amountSize: 26)
                .padding(.top, 12)
            Spacer(minLength: 4)
            if let overall = balances.overall, !balances.isSettled {
                VStack(alignment: .leading, spacing: 0) {
                    if let owed = overall.owed { Text("Owe you \(owed)") }
                    if let owing = overall.owing { Text("You owe \(owing)") }
                }
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

/// The overall balance on the left, "Updated at 14:32" under it, and the three groups most recently active on the
/// right, each with its sign and word.
struct BalanceMediumView: View {
    let balances: WidgetBalances

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 0) {
                BalanceWidgetHeader()
                OverallBalanceView(balances: balances, amountSize: 24)
                    .padding(.top, 12)
                Spacer(minLength: 4)
                if !balances.updatedTime.isEmpty {
                    Text("Updated at \(balances.updatedTime)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
            }
            .frame(width: 130, alignment: .leading)
            .frame(maxHeight: .infinity, alignment: .topLeading)
            if !balances.isSettled, !balances.groups.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(balances.groups.prefix(3).enumerated()), id: \.element.id) { index, group in
                        if index > 0 { Divider() }
                        WidgetGroupRow(group: group)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

/// The Lock Screen's circle: the logo over "+186", whose color the Lock Screen drops, so the sign says which way.
struct BalanceCircularView: View {
    let balances: WidgetBalances

    var body: some View {
        ZStack {
            AccessoryWidgetBackground()
            VStack(spacing: 1) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 13, height: 13)
                    .accessibilityHidden(true)
                if let overall = balances.overall, !balances.isSettled {
                    Text(verbatim: overall.compact)
                        .font(.system(size: 13, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .accessibilityLabel(Text(verbatim: overall.signed))
                } else if balances.isKnown {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .accessibilityLabel(Text("All settled"))
                } else {
                    Text(verbatim: "–").font(.system(size: 13, weight: .bold))
                }
            }
            .padding(4)
        }
        .accessibilityElement(children: .combine)
    }
}

/// The Lock Screen's rectangle: the app's name, "+R$ 186,40 to get back" and the other way's total.
struct BalanceRectangularView: View {
    let balances: WidgetBalances

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Blitz Split")
                .font(.caption.weight(.semibold))
            if let overall = balances.overall, !balances.isSettled {
                AmountAndWord(signed: overall.signed, direction: overall.direction)
                    .font(.subheadline.weight(.bold))
                    .monospacedDigit()
                    .widgetAccentable()
                if let other = otherLine(overall) {
                    Text(other).font(.caption)
                }
            } else if balances.isKnown {
                Text("All settled")
                    .font(.subheadline.weight(.bold))
                    .widgetAccentable()
            } else {
                Text(verbatim: "–").font(.subheadline.weight(.bold))
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.8)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Whom you owe under what comes back, and whom you're owed under what you owe.
    private func otherLine(_ overall: WidgetBalances.Overall) -> LocalizedStringKey? {
        if overall.direction == .youOwe, let owed = overall.owed { return "Owe you \(owed)" }
        if let owing = overall.owing { return "You owe \(owing)" }
        if let owed = overall.owed { return "Owe you \(owed)" }
        return nil
    }
}

/// "Balances" in the accent with the logo, at the top of the Home Screen's faces.
private struct BalanceWidgetHeader: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 6) {
            Image("Logo")
                .resizable()
                .scaledToFit()
                .frame(width: 14, height: 14)
                .accessibilityHidden(true)
            Text("Balances")
                .font(.footnote.weight(.semibold))
                .lineLimit(1)
        }
        .foregroundStyle(WidgetPalette.accent(colorScheme))
        .widgetAccentable()
    }
}

/// "+R$ 186,40" over "to get back", in the money's color; "All settled" with nothing open.
private struct OverallBalanceView: View {
    let balances: WidgetBalances
    let amountSize: CGFloat

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if let overall = balances.overall, !balances.isSettled {
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: overall.signed)
                    .font(.system(size: amountSize, weight: .bold))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                if let word = WidgetWords.word(overall.direction) {
                    Text(word)
                        .font(.footnote)
                        .lineLimit(1)
                }
            }
            .foregroundStyle(WidgetPalette.balance(overall.direction, colorScheme))
            .widgetAccentable()
        } else if balances.isKnown {
            Text("All settled")
                .font(.system(size: 20, weight: .bold))
                .lineLimit(2)
                .minimumScaleFactor(0.7)
        } else {
            Text(verbatim: "–")
                .font(.system(size: amountSize, weight: .bold))
                .foregroundStyle(.secondary)
        }
    }
}

/// A group of the medium face: its name over "−R$ 98 you owe".
private struct WidgetGroupRow: View {
    let group: WidgetBalances.Group

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: group.name)
                .font(.footnote)
                .lineLimit(1)
            AmountAndWord(signed: group.signed, direction: group.direction)
                .font(.footnote.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(WidgetPalette.balance(group.direction, colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

/// "−R$ 98 you owe": the amount and its word on one line, as the app's group rows write them.
private struct AmountAndWord: View {
    let signed: String
    let direction: WidgetBalances.Direction

    var body: some View {
        HStack(spacing: 4) {
            Text(verbatim: signed)
            if let word = WidgetWords.word(direction) { Text(word) }
        }
    }
}

/// The words beside an amount, the app's own ("to get back", "you owe"); none beside a zero, which only an overall
/// balance whose two ways offset each other shows, with "Owe you" and "You owe" under it saying what is still open.
enum WidgetWords {
    static func word(_ direction: WidgetBalances.Direction) -> LocalizedStringKey? {
        switch direction {
        case .toReceive: "to get back"
        case .youOwe: "you owe"
        case .settled: nil
        }
    }
}

/// The app's tokens (`Palette`, docs/product/general.md): the money's green and orange and the accent, light and dark.
enum WidgetPalette {
    static func accent(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(widgetHex: 0x95CDF7) : Color(widgetHex: 0x266489)
    }

    static func balance(_ direction: WidgetBalances.Direction, _ scheme: ColorScheme) -> Color {
        switch direction {
        case .toReceive: scheme == .dark ? Color(widgetHex: 0x6EDBAA) : Color(widgetHex: 0x006C4C)
        case .youOwe: scheme == .dark ? Color(widgetHex: 0xFFB599) : Color(widgetHex: 0x8E4D31)
        case .settled: .secondary
        }
    }
}

private extension Color {
    init(widgetHex rgb: UInt32) {
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255,
        )
    }
}

private extension View {
    /// The words in the app's language (Profile › Language), mirrored for Arabic and Hebrew, as the app's windows.
    @ViewBuilder
    func appLanguage(_ identifier: String?) -> some View {
        if let identifier {
            let locale = Locale(identifier: identifier)
            environment(\.locale, locale)
                .environment(
                    \.layoutDirection,
                    locale.language.characterDirection == .rightToLeft ? .rightToLeft : .leftToRight,
                )
        } else {
            self
        }
    }
}
