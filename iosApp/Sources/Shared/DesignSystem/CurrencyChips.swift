import Shared
import SwiftUI

/// One chip per currency the group has bills or payments in, labeled with its code ("BRL", "USD"), the picked one
/// filled. Shown only when there are two or more: a group in one currency has nothing to pick.
struct CurrencyChips: View {
    let currencies: [GroupCurrency]
    let selected: GroupCurrency
    let onSelect: (GroupCurrency) -> Void

    var body: some View {
        if currencies.count > 1 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(currencies, id: \.self) { currency in
                        chip(currency, isOn: currency == selected)
                    }
                }
                .padding(.horizontal, 1)
            }
            .scrollClipDisabled()
        }
    }

    private func chip(_ currency: GroupCurrency, isOn: Bool) -> some View {
        Button {
            onSelect(currency)
        } label: {
            Text(verbatim: currency.code)
                .font(.subheadline.weight(isOn ? .semibold : .regular))
                .foregroundStyle(isOn ? Palette.onAccent : Color.primary)
                .padding(.horizontal, 14)
                .frame(minHeight: 34)
                .background(isOn ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.fill.tertiary), in: .capsule)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}
