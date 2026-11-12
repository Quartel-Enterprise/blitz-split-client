import Shared
import SwiftUI

/// The options of a line of the group's Expenses, without opening it first: a bill's are BillMenuItems, a payment's
/// is Delete, which asks first too.
struct DayEntryMenuItems: View {
    let entry: any DayEntry
    let model: GroupDetailModel

    var body: some View {
        if entry is PaymentRow {
            Button(role: .destructive) {
                model.delete(entry)
            } label: {
                BillMenuDeleteLabel()
            }
        } else {
            BillMenuItems(
                onEdit: { model.send(GroupDetailUiEventOnExpenseEditClicked(expenseId: entry.id)) },
                onDuplicate: { model.send(GroupDetailUiEventOnExpenseDuplicateClicked(expenseId: entry.id)) },
                onShare: { model.send(GroupDetailUiEventOnExpenseShareClicked(expenseId: entry.id)) },
                onDelete: { model.delete(entry) },
            )
        }
    }
}

/// A line's "⋯", at its end, as the members' rows have it.
struct DayEntryMenuButton: View {
    let entry: any DayEntry
    let model: GroupDetailModel

    var body: some View {
        RowMenuButton(title: DayEntryCopy.title(entry)) {
            DayEntryMenuItems(entry: entry, model: model)
        }
    }
}

/// What a line of Expenses is called: the bill's description, or "Ana paid you" for a payment.
enum DayEntryCopy {
    static func title(_ entry: any DayEntry) -> String {
        if let payment = entry as? PaymentRow { return PaymentCopy.title(payment) }
        return (entry as? ExpenseRow)?.description_ ?? ""
    }
}
