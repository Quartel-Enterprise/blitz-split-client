import Shared
import SwiftUI

/// The options of a line of the group's Expenses, without opening it first: a bill's are the Bill toolbar's own
/// (Edit, Share, Delete, which asks first), a payment's is Delete, which asks first too. The row's "⋯", its long press
/// on iPhone and iPad, and its right click on the Mac all show these.
struct DayEntryMenuItems: View {
    let entry: any DayEntry
    let model: GroupDetailModel

    var body: some View {
        if entry is PaymentRow {
            Button(role: .destructive) {
                model.delete(entry)
            } label: {
                deleteLabel
            }
        } else {
            Button(editTitle, systemImage: "pencil") {
                model.send(GroupDetailUiEventOnExpenseEditClicked(expenseId: entry.id))
            }
            Button(shareTitle, systemImage: "square.and.arrow.up") {
                model.send(GroupDetailUiEventOnExpenseShareClicked(expenseId: entry.id))
            }
            Divider()
            Button(role: .destructive) {
                model.delete(entry)
            } label: {
                deleteLabel
            }
        }
    }

    // The Mac's items that open a sheet or a dialog end in "…"
    private var editTitle: LocalizedStringKey {
        #if os(macOS)
        "Edit…"
        #else
        "Edit"
        #endif
    }

    private var shareTitle: LocalizedStringKey {
        #if os(macOS)
        "Share…"
        #else
        "Share"
        #endif
    }

    /// "Delete" in Bill's own words, "Apagar" in Portuguese, for a bill and a payment alike.
    private var deleteLabel: some View {
        Label {
            #if os(macOS)
            Text(LocalizedStringResource("Delete… (a bill)", defaultValue: "Delete…"))
            #else
            Text(BillCopy.delete)
            #endif
        } icon: {
            Image(systemName: "trash")
        }
    }
}

/// A line's "⋯", at its end, as the members' rows have it.
struct DayEntryMenuButton: View {
    let entry: any DayEntry
    let model: GroupDetailModel

    var body: some View {
        Menu {
            DayEntryMenuItems(entry: entry, model: model)
        } label: {
            Image(systemName: "ellipsis")
                .foregroundStyle(.secondary)
                .frame(width: 32, height: 32)
                .contentShape(.rect)
        }
        .menuStyle(.button)
        .buttonStyle(.borderless)
        .menuIndicator(.hidden)
        .accessibilityLabel(Text("Options for \(DayEntryCopy.title(entry))"))
    }
}

/// What a line of Expenses is called: the bill's description, or "Ana paid you" for a payment.
enum DayEntryCopy {
    static func title(_ entry: any DayEntry) -> String {
        if let payment = entry as? PaymentRow { return PaymentCopy.title(payment) }
        return (entry as? ExpenseRow)?.description_ ?? ""
    }
}
