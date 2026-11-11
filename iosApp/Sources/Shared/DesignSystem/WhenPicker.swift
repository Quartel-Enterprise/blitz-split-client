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
        WhenValueButton(text: ExpenseFormat.when(millis, relativeDay: relativeDay), isOpen: $isOpen, onOpen: onOpen)
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

/// A bill's "When" (beta feedback): its date alone, which opens the calendar inline under it, and "Time", a switch
/// that adds the bill's optional time with its picker beside it. A bill without a time is at its day's first instant.
struct BillWhenPickerRows: View {
    let millis: Int64
    let relativeDay: RelativeDay
    @Binding var day: Date
    @Binding var time: Date
    @Binding var hasTime: Bool
    let latest: Date
    let onOpen: () -> Void

    @State private var isOpen = false
    @State private var isTimeShown = false

    var body: some View {
        WhenValueButton(
            text: ExpenseFormat.when(millis, relativeDay: relativeDay, hasTime: false),
            isOpen: $isOpen,
            onOpen: onOpen,
        )
        if isOpen {
            DatePicker("When", selection: $day, in: ...latest, displayedComponents: [.date])
                .datePickerStyle(.graphical)
                .labelsHidden()
        }
        HStack {
            Text("Time")
            Spacer()
            if hasTime || isTimeShown {
                DatePicker("Time", selection: $time, in: ...latest, displayedComponents: [.hourAndMinute])
                    .labelsHidden()
            }
            Toggle("Time", isOn: timeSwitch(hasTime: $hasTime, isShown: $isTimeShown))
                .labelsHidden()
        }
        .onChange(of: hasTime, initial: true) { _, has in
            if has { isTimeShown = true }
        }
    }
}

/// The value of "When", which opens its calendar inline under it and tells the ViewModel first.
private struct WhenValueButton: View {
    let text: String
    @Binding var isOpen: Bool
    let onOpen: () -> Void

    var body: some View {
        Button {
            if !isOpen { onOpen() }
            withAnimation { isOpen.toggle() }
        } label: {
            HStack {
                Text("When")
                    .foregroundStyle(.primary)
                Spacer()
                Text(verbatim: text)
                    .foregroundStyle(isOpen ? Palette.accent : .secondary)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
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
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        LabeledContent("When:") {
            Button(ExpenseFormat.when(millis, relativeDay: relativeDay)) {
                onOpen()
                isOpen = true
            }
            // A form locked while its popover is open (a receipt being read) closes it, so nothing picked is lost
            .onChange(of: isEnabled) { _, enabled in
                if !enabled { isOpen = false }
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
/// A bill's "When:" on the Mac (beta feedback): a button with its date, which opens the calendar in a popover, and a
/// "Time" checkbox that adds the bill's optional time with its field beside it.
struct MacBillWhenField: View {
    let millis: Int64
    let relativeDay: RelativeDay
    @Binding var day: Date
    @Binding var time: Date
    @Binding var hasTime: Bool
    let latest: Date
    let onOpen: () -> Void

    @State private var isOpen = false
    @State private var isTimeShown = false
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        LabeledContent("When:") {
            HStack(spacing: 12) {
                Button(ExpenseFormat.when(millis, relativeDay: relativeDay, hasTime: false)) {
                    onOpen()
                    isOpen = true
                }
                // A form locked while its popover is open (a receipt being read) closes it, so nothing picked is lost
                .onChange(of: isEnabled) { _, enabled in
                    if !enabled { isOpen = false }
                }
                .popover(isPresented: $isOpen, arrowEdge: .bottom) {
                    DatePicker("When", selection: $day, in: ...latest, displayedComponents: [.date])
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                        .padding()
                }
                Toggle("Time", isOn: timeSwitch(hasTime: $hasTime, isShown: $isTimeShown))
                if hasTime || isTimeShown {
                    DatePicker("Time", selection: $time, in: ...latest, displayedComponents: [.hourAndMinute])
                        .datePickerStyle(.stepperField)
                        .labelsHidden()
                }
            }
            .onChange(of: hasTime, initial: true) { _, has in
                if has { isTimeShown = true }
            }
        }
    }
}
#endif

/// A bill's "Time" switch: the time picker stays while it is turned, even through 00:00, which the bill reads as no
/// time, so it never vanishes under the finger; only the switch turned off takes it away.
private func timeSwitch(hasTime: Binding<Bool>, isShown: Binding<Bool>) -> Binding<Bool> {
    Binding(
        get: { hasTime.wrappedValue || isShown.wrappedValue },
        set: { isOn in
            isShown.wrappedValue = isOn
            hasTime.wrappedValue = isOn
        },
    )
}
