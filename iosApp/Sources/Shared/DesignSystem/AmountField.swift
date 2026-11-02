import Shared
import SwiftUI

/// The amount of a bill or of a payment, with the group currency's symbol in front: large, on the decimal pad, on
/// iPhone and iPad; a plain field on the Mac. Every keystroke goes to the ViewModel, which hands the text back.
struct AmountField: View {
    let currency: GroupCurrency
    @Binding var text: String
    let label: LocalizedStringKey

    var body: some View {
        #if os(iOS)
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(verbatim: currency.symbol)
                .font(.title2.weight(.semibold))
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            KeptTextField(text: $text, prompt: Text(verbatim: currency.zeroPlaceholder)) {
                Text(label)
            }
            .font(.system(size: 34, weight: .bold))
            .monospacedDigit()
            .keyboardType(.decimalPad)
        }
        #else
        HStack(spacing: 6) {
            Text(verbatim: currency.symbol)
                .foregroundStyle(.secondary)
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
}
