import SwiftUI

/// "Pick dates" of Statistics and of All expenses' Period: the first and the last day, never after today.
struct DateRangeSheet: View {
    let onPick: (Int64, Int64) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var start: Date
    @State private var end: Date

    init(startMillis: Int64, endMillis: Int64, onPick: @escaping (Int64, Int64) -> Void) {
        self.onPick = onPick
        _start = State(initialValue: ExpenseFormat.date(of: startMillis))
        _end = State(initialValue: ExpenseFormat.date(of: endMillis))
    }

    var body: some View {
        NavigationStack {
            Form {
                DatePicker("From", selection: $start, in: ...Date.now, displayedComponents: .date)
                DatePicker("To", selection: $end, in: ...Date.now, displayedComponents: .date)
            }
            .formStyle(.grouped)
            .navigationTitle("Pick dates")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") {
                        onPick(ExpenseFormat.millis(of: start), ExpenseFormat.millis(of: end))
                    }
                }
            }
        }
        #if os(iOS)
        .presentationDetents([.medium])
        // An iPhone Duo open: in the half before the fold, never over it
        .foldSheetPlacement()
        #else
        .frame(width: 360, height: 220)
        #endif
    }
}
