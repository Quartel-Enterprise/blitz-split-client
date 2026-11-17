import Shared
import SwiftUI

/// "Group reminder" (New group and Edit group): the weekly "Open balance reminder" every group keeps, Monday at 10:00
/// unless it is changed here: another weekday and time, a day of the month and a time, or off. Read on each member's
/// own clock. `calendar` is the row that adds it to the device's calendar.
struct GroupReminderSection<CalendarRow: View>: View {
    let reminder: GroupReminder
    let isDisabled: Bool
    let onEnabled: (Bool) -> Void
    let onKind: (ReminderKind) -> Void
    let onDay: (Int) -> Void
    let onMinute: (Int) -> Void
    @ViewBuilder let calendar: () -> CalendarRow

    var body: some View {
        Section {
            Toggle("Remind members to settle up", isOn: Binding(get: { reminder.isEnabled }, set: onEnabled))
                .tint(Palette.accent)
            if reminder.isEnabled {
                Picker("Repeats", selection: Binding(get: { reminder.kind }, set: onKind)) {
                    Text("Weekly").tag(ReminderKind.weekly)
                    Text("Monthly").tag(ReminderKind.monthly)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                dayPicker
                DatePicker(
                    "Time",
                    selection: Binding(
                        get: { ReminderCopy.date(minute: Int(reminder.minute)) },
                        set: { onMinute(ReminderCopy.minute(of: $0)) },
                    ),
                    displayedComponents: .hourAndMinute,
                )
                calendar()
            }
        } header: {
            Text("Group reminder")
        } footer: {
            Text("Members who owe get a notification, on their own clock.")
        }
        .disabled(isDisabled)
    }

    @ViewBuilder private var dayPicker: some View {
        if reminder.kind == ReminderKind.weekly {
            Picker("Day", selection: Binding(get: { Int(reminder.day) }, set: onDay)) {
                ForEach(1...7, id: \.self) { day in
                    Text(verbatim: ReminderCopy.weekday(day)).tag(day)
                }
            }
        } else {
            Picker("Day of the month", selection: Binding(get: { Int(reminder.day) }, set: onDay)) {
                ForEach(1...31, id: \.self) { day in
                    Text(verbatim: "\(day)").tag(day)
                }
            }
        }
    }
}

enum ReminderCopy {
    /// An ISO weekday (1 Monday to 7 Sunday) by name, in the app's language.
    static func weekday(_ isoDay: Int) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: Bundle.appLanguageIdentifier ?? Locale.current.identifier)
        return calendar.standaloneWeekdaySymbols[isoDay % 7]
    }

    static func date(minute: Int) -> Date {
        Calendar.current.date(bySettingHour: minute / 60, minute: minute % 60, second: 0, of: Date()) ?? Date()
    }

    static func minute(of date: Date) -> Int {
        let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
        return (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
    }

    private static func time(_ minute: Int) -> String {
        date(minute: minute).formatted(date: .omitted, time: .shortened)
    }

    /// "Every Monday at 10:00" or "Every month on day 25 at 12:00".
    static func schedule(_ reminder: GroupReminder) -> String {
        let day = Int(reminder.day)
        let at = time(Int(reminder.minute))
        if reminder.kind == ReminderKind.weekly {
            return String(localized: "Every \(weekday(day)) at \(at)", bundle: .appLanguage)
        }
        return String(localized: "Every month on day \(day) at \(at)", bundle: .appLanguage)
    }
}
