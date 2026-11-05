import Shared
import SwiftUI

/// "How it works" of "Simplify debts" (13.2): what it is, the example of two payments becoming one, and what is good
/// to know. The same words for everyone, the example's amounts in the group's currency. Pushed inside Edit group's
/// sheet, or over the group from Balances' card; the Mac shows it as a sheet from the group. No ViewModel: back (Done
/// on the Mac) pops it as any pushed screen or sheet.
struct SimplifyDebtsInfoView: View {
    let route: RouteSimplifyDebtsInfo

    @Environment(\.dismiss) private var dismiss

    private var currency: GroupCurrency {
        route.currency
    }

    /// An example amount in the route's currency: 20, 121.40.
    private func amount(_ major: Double) -> String {
        let minor = (major * pow(10, Double(currency.fractionDigits))).rounded()
        return currency.format(Int64(minor))
    }

    var body: some View {
        Form {
            Section {
                Text("In a group, each bill creates debts between two people. Over time, money goes in circles. Simplifying combines those debts and suggests fewer payments.")
                    .fixedSize(horizontal: false, vertical: true)
            } header: {
                Text("What it is")
            }
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    Text(verbatim: String(
                        localized: "Carla owes Elisa \(amount(20)), and Elisa owes you \(amount(20)).",
                        bundle: .appLanguage,
                    ))
                        .fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 10) {
                        SimplifyExampleFigure(isSimplified: false, amount: amount(20))
                        SimplifyExampleFigure(isSimplified: true, amount: amount(20))
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Text("Example: 2 payments become 1"))
                    Text(verbatim: String(
                        localized: "Simplified, Carla pays you \(amount(20)) directly, and Elisa pays nothing.",
                        bundle: .appLanguage,
                    ))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.vertical, 4)
            } header: {
                Text("An example")
            }
            Section {
                SimplifyFact(
                    systemImage: "equal.circle",
                    title: "Totals don't change",
                    text: String(
                        localized: "Whoever owed \(amount(121.40)) in the group still owes \(amount(121.40)). Only where the money goes changes.",
                        bundle: .appLanguage,
                    ),
                )
                SimplifyFact(
                    systemImage: "eye",
                    title: "You may owe someone you never split a bill with",
                    text: String(
                        localized: "In the group's Balances, the Simplified debts label tells you when that happens.",
                        bundle: .appLanguage,
                    ),
                )
                SimplifyFact(
                    systemImage: "person.2",
                    title: "Anyone can turn it on or off",
                    text: String(
                        localized: "In Edit group. Activity shows who changed it, and whoever now pays or receives from someone else gets a notification.",
                        bundle: .appLanguage,
                    ),
                )
                SimplifyFact(
                    systemImage: "arrow.counterclockwise",
                    title: "Turning it off goes back",
                    text: String(
                        localized: "Payments go back to being between whoever split each bill. No bill or recorded payment changes.",
                        bundle: .appLanguage,
                    ),
                )
            } header: {
                Text("Good to know")
            }
        }
        .formStyle(.grouped)
        .navigationTitle("Simplify debts")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.large)
        #else
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") { dismiss() }
            }
        }
        .frame(minWidth: 460, idealWidth: 520, minHeight: 400, idealHeight: 600)
        #endif
    }
}

/// One fact of "Good to know": its icon, its bold title and the text.
private struct SimplifyFact: View {
    let systemImage: String
    let title: LocalizedStringKey
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage)
                .font(.body.weight(.semibold))
                .foregroundStyle(Palette.accent)
                .frame(width: 30, height: 30)
                .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 8))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                Text(verbatim: text)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

/// The example, before or simplified: Carla top left, Elisa top right, you at the bottom, and the arrows of who pays
/// whom with the amount, in the boards' 160×130 frame.
private struct SimplifyExampleFigure: View {
    let isSimplified: Bool
    let amount: String

    private static let carla = CGPoint(x: 30, y: 30)
    private static let elisa = CGPoint(x: 130, y: 30)
    private static let you = CGPoint(x: 80, y: 104)
    private static let scale: CGFloat = 0.8

    var body: some View {
        VStack(spacing: 4) {
            Text(caption)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(isSimplified ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.secondary))
                .multilineTextAlignment(.center)
            ZStack(alignment: .topLeading) {
                if isSimplified {
                    arrow(from: CGPoint(x: 38, y: 46), to: CGPoint(x: 68, y: 90))
                    label(at: CGPoint(x: 28, y: 76))
                } else {
                    arrow(from: CGPoint(x: 48, y: 30), to: CGPoint(x: 111, y: 30))
                    label(at: CGPoint(x: 80, y: 16))
                    arrow(from: CGPoint(x: 122, y: 46), to: CGPoint(x: 92, y: 90))
                    label(at: CGPoint(x: 132, y: 76))
                }
                face("C", color: .lilac, at: Self.carla)
                face("E", color: .sand, at: Self.elisa)
                    .opacity(isSimplified ? 0.45 : 1)
                face(nil, color: .blue, at: Self.you)
            }
            .frame(width: 160, height: 130, alignment: .topLeading)
            // The boards' 140 pt figure, so the two fit side by side in an iPhone's row
            .scaleEffect(Self.scale)
            .frame(width: 160 * Self.scale, height: 130 * Self.scale)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity)
        .background(Palette.groupedBackground, in: .rect(cornerRadius: 18))
    }

    private var caption: LocalizedStringKey {
        isSimplified ? "Simplified · 1 payment" : "Before · 2 payments"
    }

    private func face(_ initial: String?, color: AvatarColor, at point: CGPoint) -> some View {
        Group {
            if let initial {
                Text(verbatim: initial)
                    .font(.system(size: 12, weight: .bold))
            } else {
                Image(systemName: "person.fill")
                    .font(.system(size: 13, weight: .bold))
            }
        }
        .foregroundStyle(color.content)
        .frame(width: 32, height: 32)
        .background(color.container, in: .circle)
        .position(point)
    }

    private func label(at point: CGPoint) -> some View {
        Text(verbatim: amount)
            .font(.system(size: 10, weight: .semibold))
            .monospacedDigit()
            .foregroundStyle(Palette.accent)
            .fixedSize()
            .position(point)
    }

    private func arrow(from start: CGPoint, to end: CGPoint) -> some View {
        Path { path in
            path.move(to: start)
            path.addLine(to: end)
            let angle = atan2(end.y - start.y, end.x - start.x)
            let head: CGFloat = 8
            for side in [-1.0, 1.0] {
                let wing = angle + .pi + side * .pi / 6
                path.move(to: end)
                path.addLine(to: CGPoint(x: end.x + head * cos(wing), y: end.y + head * sin(wing)))
            }
        }
        .stroke(Palette.accent, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
    }
}
