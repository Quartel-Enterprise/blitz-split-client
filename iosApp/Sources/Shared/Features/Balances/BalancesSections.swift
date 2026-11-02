import Shared
import SwiftUI

/// "Balances", the group's tab with who owes whom, each pair once and already netted, under the group's header:
/// "Nobody owes anything yet" before the first bill, "Everyone's settled up" once nobody owes, then "With you" and
/// "Between the others", and the note on how the pairs are netted. A pair opens "Record payment" over the group.
struct BalancesSections: View {
    let model: GroupDetailModel

    private var state: GroupDetailUiState {
        model.state
    }

    private var currency: GroupCurrency {
        state.header.currency
    }

    var body: some View {
        if state.isBalancesEmpty {
            Section {
                ContentUnavailableView(
                    "Nobody owes anything yet",
                    systemImage: "arrow.left.arrow.right",
                    description: Text("Balances show up once the first expense is added."),
                )
            }
            .listRowBackground(Color.clear)
        } else if state.header.isEveryoneSettled {
            Section {
                EveryoneSettled(canDelete: state.exit.canLeaveOrDelete) {
                    model.send(GroupDetailUiEventOnDeleteGroupClicked.shared)
                }
            }
        }
        if !state.myDebts.isEmpty {
            Section {
                debtRows(state.myDebts)
            } header: {
                HStack {
                    Text("With you")
                    Spacer()
                    if let balance = state.header.myBalance {
                        Text(verbatim: myHint(balance))
                            .textCase(nil)
                    }
                }
            }
        }
        if !state.otherDebts.isEmpty {
            Section {
                debtRows(state.otherDebts)
            } header: {
                HStack {
                    Text("Between the others")
                    Spacer()
                    Text("\(state.otherDebts.count) to settle")
                        .textCase(nil)
                }
            }
        }
        if state.showsDebtNote {
            Section {
                Text("Each pair's debts are already netted: if Ana owes you and you owe her, only the difference shows. “Got paid” and “Already paid” record a payment made outside the app (Pix, cash, a transfer).")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .listRowBackground(Color.clear)
        }
    }

    private func debtRows(_ rows: [DebtRow]) -> some View {
        ForEach(rows, id: \.self) { row in
            DebtRowView(row: row, currency: currency) {
                model.send(GroupDetailUiEventOnDebtClicked(fromMemberId: row.fromMemberId, toMemberId: row.toMemberId))
            }
        }
    }

    /// "get $332.90 · owe $118.00", without the part that is zero.
    private func myHint(_ balance: MemberBalance) -> String {
        let receives = currency.format(balance.receivesMinor)
        let owes = currency.format(balance.owesMinor)
        if balance.owesMinor == 0 { return String(localized: "get \(receives)") }
        if balance.receivesMinor == 0 { return String(localized: "owe \(owes)") }
        return String(localized: "get \(receives) · owe \(owes)")
    }
}

/// "Everyone's settled up", which offers deleting the group once the trip is over; offline it needs the internet.
private struct EveryoneSettled: View {
    let canDelete: Bool
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text("Everyone's settled up")
                    .font(.headline)
            } icon: {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(Palette.accent)
            }
            Text("Nobody owes anything in this group. If the trip is over, you can delete the group for everyone.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Button("Delete group", role: .destructive, action: onDelete)
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .disabled(!canDelete)
        }
        .padding(.vertical, 4)
    }
}

/// How a pair reads, in Balances and in the dialogs of "Leave group" and "Delete group".
enum DebtCopy {
    static func sentence(_ row: DebtRow) -> String {
        let from = PaymentCopy.name(row.fromName)
        let to = PaymentCopy.name(row.toName)
        switch row.kind {
        case .youOwe:
            // "You owe $118.00" is the balance card's: Portuguese words the two apart ("Você deve a Bruno")
            return String(
                localized: "You owe %@ (a member)",
                defaultValue: "You owe \(to)",
                comment: "A pair in Balances; %@ is whom this device's user owes",
            )
        case .owesYou:
            return String(localized: "\(from) owes you")
        default:
            return String(localized: "\(from) owes \(to)")
        }
    }

    static func amount(_ row: DebtRow, currency: GroupCurrency) -> String {
        switch row.kind {
        case .youOwe: currency.formatSigned(-row.amountMinor)
        case .owesYou: currency.formatSigned(row.amountMinor)
        default: currency.format(row.amountMinor)
        }
    }
}

/// A pair: the two faces, "You owe Bruno", "Ana owes you" or "Diego owes Bruno", the amount and the button that
/// records the payment ("Settle up", "Got paid" or "Already paid"). The whole row opens Record payment.
private struct DebtRowView: View {
    let row: DebtRow
    let currency: GroupCurrency
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                DebtFaces(from: row.fromFace, to: row.toFace)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: sentence)
                        .lineLimit(2)
                    Text(verbatim: amount)
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(amountColor)
                }
                Spacer(minLength: 8)
                Text(buttonTitle)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(row.kind == .youOwe ? Palette.onAccent : Palette.accent)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(row.kind == .youOwe ? Palette.accent : Palette.accent.opacity(0.14), in: .capsule)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: "\(sentence), \(amount)"))
        .accessibilityHint(Text(buttonTitle))
        .accessibilityAddTraits(.isButton)
    }

    private var sentence: String {
        DebtCopy.sentence(row)
    }

    private var amount: String {
        DebtCopy.amount(row, currency: currency)
    }

    private var amountColor: Color {
        switch row.kind {
        case .youOwe: Palette.negative
        case .owesYou: Palette.accent
        default: .secondary
        }
    }

    private var buttonTitle: LocalizedStringKey {
        switch row.kind {
        case .youOwe: "Settle up"
        case .owesYou: "Got paid"
        default: "Already paid"
        }
    }
}

/// Whoever pays over whoever receives; a blank avatar for someone no longer in the group.
private struct DebtFaces: View {
    let from: GroupFace?
    let to: GroupFace?
    var size: CGFloat = 32

    var body: some View {
        HStack(spacing: -size * 0.3) {
            face(from)
            face(to)
                .overlay(Circle().strokeBorder(Palette.card, lineWidth: 2))
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func face(_ face: GroupFace?) -> some View {
        if let face {
            AvatarPhoto(initial: face.initials, color: face.color, photoUrl: nil, size: size)
        } else {
            Circle()
                .fill(.fill.tertiary)
                .frame(width: size, height: size)
        }
    }
}
