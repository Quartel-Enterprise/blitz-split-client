import Shared
import SwiftUI

// "When" of a bill or of a payment: "Today, Oct 28 · 9:30 PM", and a date and time picker that stops at `latest`
// (it happened by now). `onOpen` tells the ViewModel the picker opened, so it moves `latest` to now first.

#if os(iOS)
/// Two rows of a form section: the value, which opens the calendar inline under it, and the calendar.
struct WhenPickerRows: View {
    let millis: Int64
    let relativeDay: RelativeDay
    @Binding var selection: Date
    let latest: Date
    let onOpen: () -> Void

    @State private var isOpen = false

    var body: some View {
        Button {
            if !isOpen { onOpen() }
            withAnimation { isOpen.toggle() }
        } label: {
            HStack {
                Text("When")
                    .foregroundStyle(.primary)
                Spacer()
                Text(verbatim: ExpenseFormat.when(millis, relativeDay: relativeDay))
                    .foregroundStyle(isOpen ? Palette.accent : .secondary)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        if isOpen {
            DatePicker(
                "When",
                selection: $selection,
                in: ...latest,
                displayedComponents: [.date, .hourAndMinute],
            )
            .datePickerStyle(.graphical)
            .labelsHidden()
        }
    }
}
#else
/// "When:" of a Mac form: a button with the value, which opens the calendar and the time in a popover.
struct MacWhenField: View {
    let millis: Int64
    let relativeDay: RelativeDay
    @Binding var selection: Date
    let latest: Date
    let onOpen: () -> Void

    @State private var isOpen = false

    var body: some View {
        LabeledContent("When:") {
            Button(ExpenseFormat.when(millis, relativeDay: relativeDay)) {
                onOpen()
                isOpen = true
            }
            .popover(isPresented: $isOpen, arrowEdge: .bottom) {
                VStack(spacing: 8) {
                    DatePicker(
                        "When",
                        selection: $selection,
                        in: ...latest,
                        displayedComponents: [.date],
                    )
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    DatePicker(
                        "Time",
                        selection: $selection,
                        in: ...latest,
                        displayedComponents: [.hourAndMinute],
                    )
                    .datePickerStyle(.stepperField)
                }
                .padding()
            }
        }
    }
}
#endif
