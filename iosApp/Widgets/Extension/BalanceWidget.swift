import SwiftUI
import WidgetKit

@main
struct BalanceWidgetBundle: WidgetBundle {
    var body: some Widget {
        BalanceWidget()
    }
}

/// The balance widget (board 15.8): the overall balance and the open groups, as the app last wrote them. Tapping it
/// opens Balances.
struct BalanceWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetBalancesStore.kind, provider: BalanceTimelineProvider()) { entry in
            BalanceWidgetView(balances: entry.balances)
                .containerBackground(for: .widget) { Color.card }
        }
        .configurationDisplayName("Balances")
        .description("Your overall balance and the groups still open.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular])
    }
}

struct BalanceEntry: TimelineEntry {
    let date: Date
    let balances: WidgetBalances
}

/// One entry, the file as it is: the app reloads the timeline whenever it writes it again.
struct BalanceTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> BalanceEntry {
        BalanceEntry(date: .now, balances: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (BalanceEntry) -> Void) {
        let balances = WidgetBalancesStore.read() ?? (context.isPreview ? .preview : .unknown)
        completion(BalanceEntry(date: .now, balances: balances))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<BalanceEntry>) -> Void) {
        let entry = BalanceEntry(date: .now, balances: WidgetBalancesStore.read() ?? .unknown)
        completion(Timeline(entries: [entry], policy: .never))
    }
}

#Preview("Home Screen, small", as: .systemSmall) {
    BalanceWidget()
} timeline: {
    BalanceEntry(date: .now, balances: .preview)
    BalanceEntry(date: .now, balances: .unknown)
}

#Preview("Home Screen, medium", as: .systemMedium) {
    BalanceWidget()
} timeline: {
    BalanceEntry(date: .now, balances: .preview)
    BalanceEntry(date: .now, balances: .unknown)
}
