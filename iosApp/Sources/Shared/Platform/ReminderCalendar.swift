import EventKit
import Foundation
import Shared
#if os(iOS)
import EventKitUI
import UIKit
#else
import AppKit
#endif

/// "Add to calendar": the group's reminder as a repeating event in the device's calendar. iPhone and iPad open the
/// system's event editor prefilled (it needs no permission, and the person saves or cancels it); the Mac opens an
/// .ics file in Calendar, which asks the same before adding it.
@MainActor
enum ReminderCalendar {
    static func add(_ event: ReminderCalendarEvent) {
        let title = String(localized: "Settle up: \(event.groupName)", bundle: .appLanguage)
        let notes = String(localized: "Time to settle up in \(event.groupName).", bundle: .appLanguage)
        #if os(iOS)
        present(event, title: title, notes: notes)
        #else
        openIcs(event, title: title, notes: notes)
        #endif
    }

    #if os(iOS)
    private static let editorDelegate = EditorDelegate()

    private static func present(_ event: ReminderCalendarEvent, title: String, notes: String) {
        guard let window = PresentationAnchor.current, var top = window.rootViewController else { return }
        // Over the sheet the form or the group may be in
        while let presented = top.presentedViewController, !presented.isBeingDismissed {
            top = presented
        }
        let store = EKEventStore()
        let item = EKEvent(eventStore: store)
        item.title = title
        item.notes = notes
        item.startDate = Date(timeIntervalSince1970: TimeInterval(event.startMillis) / 1000)
        item.endDate = item.startDate.addingTimeInterval(15 * 60)
        // No time zone: a floating event, as in the .ics and on Android, so it follows each person's own clock
        item.timeZone = nil
        item.addAlarm(EKAlarm(relativeOffset: 0))
        if let rule = recurrenceRule(event.rrule) { item.addRecurrenceRule(rule) }
        item.calendar = store.defaultCalendarForNewEvents
        let controller = EKEventEditViewController()
        controller.eventStore = store
        controller.event = item
        controller.editViewDelegate = editorDelegate
        top.present(controller, animated: true)
    }

    private final class EditorDelegate: NSObject, EKEventEditViewDelegate {
        func eventEditViewController(
            _ controller: EKEventEditViewController,
            didCompleteWith action: EKEventEditViewAction,
        ) {
            controller.dismiss(animated: true)
        }
    }

    /// "FREQ=WEEKLY;BYDAY=MO" or "FREQ=MONTHLY;BYMONTHDAY=25" (-1 for a 31st: each month's last day).
    private static func recurrenceRule(_ rrule: String) -> EKRecurrenceRule? {
        let parts = Dictionary(uniqueKeysWithValues: rrule.split(separator: ";").compactMap { part -> (String, String)? in
            let pair = part.split(separator: "=", maxSplits: 1)
            return pair.count == 2 ? (String(pair[0]), String(pair[1])) : nil
        })
        switch parts["FREQ"] {
        case "WEEKLY":
            let days: [String: EKWeekday] = [
                "MO": .monday, "TU": .tuesday, "WE": .wednesday, "TH": .thursday,
                "FR": .friday, "SA": .saturday, "SU": .sunday,
            ]
            guard let day = parts["BYDAY"].flatMap({ days[$0] }) else { return nil }
            return EKRecurrenceRule(
                recurrenceWith: .weekly, interval: 1, daysOfTheWeek: [EKRecurrenceDayOfWeek(day)],
                daysOfTheMonth: nil, monthsOfTheYear: nil, weeksOfTheYear: nil, daysOfTheYear: nil,
                setPositions: nil, end: nil,
            )
        case "MONTHLY":
            guard let day = parts["BYMONTHDAY"].flatMap({ Int($0) }) else { return nil }
            return EKRecurrenceRule(
                recurrenceWith: .monthly, interval: 1, daysOfTheWeek: nil,
                daysOfTheMonth: [NSNumber(value: day)], monthsOfTheYear: nil, weeksOfTheYear: nil,
                daysOfTheYear: nil, setPositions: nil, end: nil,
            )
        default:
            return nil
        }
    }
    #else
    private static func openIcs(_ event: ReminderCalendarEvent, title: String, notes: String) {
        let ics = event.toIcs(title: title, description: notes, uid: "reminder-\(event.startMillis)@blitzsplit.com")
        let url = FileManager.default.temporaryDirectory.appending(path: "blitz-split-reminder.ics")
        guard (try? ics.write(to: url, atomically: true, encoding: .utf8)) != nil else { return }
        NSWorkspace.shared.open(url)
    }
    #endif
}
