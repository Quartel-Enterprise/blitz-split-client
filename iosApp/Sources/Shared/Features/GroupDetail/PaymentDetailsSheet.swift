import Shared
import SwiftUI

/// What a tap on a payment of the group's Expenses opens, instead of asking to delete it: who paid whom, how much,
/// when and how, with "Delete" at the bottom, which asks "Delete this payment?" before anything leaves the group.
struct PaymentDetailsSheet: View {
    let payment: PaymentRow
    let model: GroupDetailModel

    var body: some View {
        List {
            Section {
                Text(verbatim: PaymentCopy.kindAndMethod(payment))
                    .foregroundStyle(.secondary)
            }
            Section {
                LabeledContent("Amount") {
                    Text(verbatim: payment.currency.format(payment.amountMinor))
                        .monospacedDigit()
                }
                LabeledContent("When") {
                    Text(verbatim: ExpenseFormat.when(payment.paidAt, relativeDay: payment.paidDay))
                }
                if payment.isPending {
                    LabeledContent("Status") { Text("Queued") }
                }
            }
            Section {
                Button(role: .destructive) {
                    model.deletePaymentFromDetails(payment)
                } label: {
                    Text(BillCopy.delete)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle(Text(verbatim: PaymentCopy.title(payment)))
        #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
        #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { model.dismissPaymentDetails() }
                }
            }
    }
}
