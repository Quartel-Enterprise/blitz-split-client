import Shared
import SwiftUI

/// The amount of a bill or of a payment, with the group currency's symbol in front: large, on the number pad, on
/// iPhone and iPad; a plain field on the Mac. Every keystroke goes to the ViewModel, which hands the text back.
struct AmountField: View {
    let currency: GroupCurrency
    @Binding var text: String
    let label: LocalizedStringKey
    /// Rachar conta's: tapping the symbol opens the same "Currency" menu as the control beside the total (beta
    /// feedback); nil keeps the symbol as plain text.
    var currencyChoice: CurrencyChoice?

    var body: some View {
        #if os(iOS)
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            symbol
                .font(.title2.weight(.semibold))
            KeptTextField(text: $text, prompt: Text(verbatim: currency.zeroPlaceholder)) {
                Text(label)
            }
            .font(.largeTitle.bold())
            .monospacedDigit()
            .keyboardType(.numberPad)
        }
        #else
        HStack(spacing: 6) {
            symbol
            KeptTextField(text: $text, prompt: Text(verbatim: currency.zeroPlaceholder)) {
                Text(label)
            }
            // The form's row already names it; the label stays for VoiceOver
            .labelsHidden()
            .monospacedDigit()
            .frame(width: 160)
        }
        #endif
    }

    @ViewBuilder private var symbol: some View {
        if let currencyChoice {
            Menu {
                Picker("Currency", selection: currencyChoice.selection) {
                    CurrencyOptions(currencies: currencyChoice.currencies)
                }
                .pickerStyle(.inline)
            } label: {
                Text(verbatim: currency.symbol)
                    .foregroundStyle(.secondary)
                    .contentShape(.rect)
            }
            .menuIndicator(.hidden)
            .menuStyle(.button)
            .buttonStyle(.plain)
            .fixedSize()
            .disabled(!currencyChoice.isEnabled)
            .accessibilityLabel(Text("Currency"))
            .accessibilityValue(Text(verbatim: currency.code))
        } else {
            Text(verbatim: currency.symbol)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
        }
    }
}

/// The currencies the symbol of `AmountField` offers, and the one picked.
struct CurrencyChoice {
    let selection: Binding<GroupCurrency>
    let currencies: [GroupCurrency]
    let isEnabled: Bool
}
