import Foundation
import Shared

/// The words of "Statistics", the same on iPhone, iPad and the Mac. Days arrive as the first instant of each day in
/// epoch millis; each is written in the device's language ("Oct 24 – 29", "September 2026", "Jul", "2025").
enum GroupStatsCopy {
    /// "The whole trip" in a trip, "All time" in any other group, and the other periods by name.
    static func period(_ period: StatsPeriod, isTrip: Bool) -> String {
        if period == .all {
            return isTrip ? String(localized: "The whole trip") : String(localized: "All time")
        }
        if period == .thisYear { return String(localized: "This year") }
        if period == .thisMonth { return String(localized: "This month") }
        if period == .lastMonth { return String(localized: "Last month") }
        return String(localized: "Pick dates…")
    }

    /// "no expenses", "1 expense" or "7 expenses".
    static func expenseCount(_ count: Int32) -> String {
        if count == 0 {
            return String(
                localized: "Statistics without expenses",
                defaultValue: "no expenses",
                comment: "How many bills a period of the statistics has, when it has none",
            )
        }
        return String(localized: "\(Int(count)) expenses")
    }

    /// Under a period of the menu: "Oct 24 – 29 · 7 expenses", or "From one day to another" before any dates.
    static func hint(_ option: StatsPeriodOption) -> String {
        guard let start = option.startMillis?.int64Value, let end = option.endMillis?.int64Value else {
            return String(localized: "From one day to another")
        }
        return "\(range(option.rangeStyle, start: start, end: end)) · \(expenseCount(option.expenseCount))"
    }

    /// "Oct 24 – 29", "Jan – Oct 2026" or "September 2026", as the shared code says the period reads.
    static func range(_ style: StatsRangeStyle, start: Int64, end: Int64) -> String {
        let first = ExpenseFormat.date(of: start)
        // An interval needs its end after its start: noon of the last day
        let last = ExpenseFormat.date(of: end).addingTimeInterval(12 * 60 * 60)
        if style == .month {
            return first.formatted(.dateTime.month(.wide).year())
        }
        if style == .days {
            let calendar = Calendar.current
            let thisYear = calendar.component(.year, from: .now)
            if calendar.component(.year, from: first) == thisYear, calendar.component(.year, from: last) == thisYear {
                return (first..<last).formatted(.interval.day().month(.abbreviated))
            }
            return (first..<last).formatted(.interval.day().month(.abbreviated).year())
        }
        return (first..<last).formatted(.interval.month(.abbreviated).year())
    }

    /// The label of a bar under the chart: the day ("today"), the week, the month or the year.
    static func barLabel(_ bar: StatsBar, grain: StatsGrain, isSparse: Bool) -> String {
        let date = ExpenseFormat.date(of: bar.startMillis)
        if grain == .day {
            return bar.isToday ? String(localized: "today") : date.formatted(.dateTime.day())
        }
        if grain == .week {
            return isSparse ? date.formatted(.dateTime.month(.abbreviated)) : week(bar)
        }
        if grain == .month { return date.formatted(.dateTime.month(.abbreviated)) }
        return date.formatted(.dateTime.year())
    }

    /// What a bar names in a sentence: "Oct 28", "Oct 26 – Nov 1", "July" or "2025".
    static func barName(_ bar: StatsBar, grain: StatsGrain) -> String {
        let date = ExpenseFormat.date(of: bar.startMillis)
        if grain == .day { return date.formatted(.dateTime.day().month(.abbreviated)) }
        if grain == .week { return week(bar) }
        if grain == .month { return date.formatted(.dateTime.month(.wide)) }
        return date.formatted(.dateTime.year())
    }

    private static func week(_ bar: StatsBar) -> String {
        let first = ExpenseFormat.date(of: bar.startMillis)
        let last = ExpenseFormat.date(of: bar.endMillis).addingTimeInterval(12 * 60 * 60)
        return (first..<last).formatted(.interval.day().month(.abbreviated))
    }

    /// "Most expensive day: Oct 28,", before the amount.
    static func peak(_ bar: StatsBar, grain: StatsGrain) -> String {
        let name = barName(bar, grain: grain)
        if grain == .week { return String(localized: "Most expensive week: \(name),") }
        if grain == .month { return String(localized: "Most expensive month: \(name),") }
        if grain == .year { return String(localized: "Most expensive year: \(name),") }
        return String(localized: "Most expensive day: \(name),")
    }

    /// The chart's whole text for VoiceOver: "Spending by day: Oct 24 $561.00; Oct 25 nothing; …".
    static func chartDescription(_ state: GroupStatsUiState) -> String {
        let nothing = String(localized: "nothing")
        let values = state.bars.map { bar in
            let amount = bar.amountMinor > 0 ? state.currency.format(bar.amountMinor) : nothing
            return "\(barName(bar, grain: state.grain)) \(amount)"
        }.joined(separator: "; ")
        if state.grain == .week { return String(localized: "Spending by week: \(values)") }
        if state.grain == .month { return String(localized: "Spending by month: \(values)") }
        if state.grain == .year { return String(localized: "Spending by year: \(values)") }
        return String(localized: "Spending by day: \(values)")
    }

    /// Why some grains are off, one sentence per reason.
    static func grainNote(_ state: GroupStatsUiState) -> String? {
        let sentences = state.grainNotes.map { note -> String in
            if note == .weekMonthAndYearNeedLongerPeriod {
                return String(localized: "Week, Month and Year show up in periods longer than a week.")
            }
            if note == .monthAndYearNeedLongerPeriod {
                return String(localized: "Month and Year show up in periods longer than a month.")
            }
            if note == .yearNeedsLongerPeriod {
                return String(localized: "Year shows up in periods longer than a year.")
            }
            if note == .yearSwitchesPeriod {
                return state.isTrip
                    ? String(localized: "To compare years, pick Year: the period becomes The whole trip.")
                    : String(localized: "To compare years, pick Year: the period becomes All time.")
            }
            return String(localized: "Day works for periods of up to 3 months.")
        }
        return sentences.isEmpty ? nil : sentences.joined(separator: " ")
    }

    /// "No expenses in September" (or "in 2026"), "in that period" for picked dates or the whole period.
    static func periodEmptyTitle(_ state: GroupStatsUiState) -> String {
        let date = ExpenseFormat.date(of: state.periodStartMillis)
        if state.period == .thisMonth || state.period == .lastMonth {
            return String(localized: "No expenses in \(date.formatted(.dateTime.month(.wide)))")
        }
        if state.period == .thisYear {
            return String(localized: "No expenses in \(date.formatted(.dateTime.year()))")
        }
        return String(localized: "No expenses in that period")
    }

    /// "Bruno (you)" for this device's user, "Former member" for someone who left the group.
    static func name(_ person: StatsPersonRow) -> String {
        guard let name = person.name else { return String(localized: "Former member") }
        return person.isMe ? String(localized: "\(name) (you)") : name
    }

    /// "Oct 28, 3:00 PM · You paid", the biggest bill's day, time and who paid it.
    static func biggestMeta(_ expense: StatsBiggestExpense) -> String {
        let when = ExpenseFormat.dayTime(expense.spentAt, relativeDay: .other)
        let payers = expense.payers.map { (name: $0.name, isMe: $0.isMe) }
        guard let paid = ExpenseFormat.paid(payers, loneFormer: nil) else { return when }
        return "\(when) · \(paid)"
    }

    static func grain(_ grain: StatsGrain) -> String {
        if grain == .week { return String(localized: "Week") }
        if grain == .month { return String(localized: "Month") }
        if grain == .year { return String(localized: "Year") }
        return String(localized: "Day")
    }
}
