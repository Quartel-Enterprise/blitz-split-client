import SwiftUI
import WidgetKit

@main
struct WatchBalanceWidgetBundle: WidgetBundle {
    var body: some Widget {
        WatchBalanceWidget()
    }
}

/// The watch's complications (15.2: the circle "+186" with the logo, the rectangle "+R$ 186,40 to get back") and its
/// Smart Stack card (15.1, the rectangle), the iPhone widget's accessory faces over the file the watch app writes
/// (`WatchWidgetWriter`). Tapping one opens the app.
struct WatchBalanceWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetBalancesStore.kind, provider: WatchBalanceTimelineProvider()) { entry in
            WatchBalanceWidgetView(balances: entry.balances)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName("Balances")
        .description("Your overall balance and the groups still open.")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

/// The accessory faces of `BalanceWidgetView`, plus the inline one of a watch face's top line ("+R$ 186,40"), and the
/// app's icon alone on a watch with no account.
private struct WatchBalanceWidgetView: View {
    let balances: WidgetBalances

    @Environment(\.widgetFamily) private var family

    var body: some View {
        if family == .accessoryInline {
            inline
        } else if !balances.isKnown {
            // No account on the watch yet (or signed out): the app's icon alone, a tap opens Connect
            ZStack {
                if family == .accessoryCircular { AccessoryWidgetBackground() }
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                    .accessibilityLabel(Text("Blitz Split"))
            }
        } else {
            BalanceWidgetView(balances: balances)
        }
    }

    @ViewBuilder
    private var inline: some View {
        if let overall = balances.overall, !balances.isSettled {
            Text(verbatim: overall.signed)
        } else if balances.isKnown {
            Text("All settled")
        } else {
            Text("Blitz Split")
        }
    }
}

struct WatchBalanceEntry: TimelineEntry {
    let date: Date
    let balances: WidgetBalances
}

/// One entry, the file as it is: the watch app reloads the timeline whenever it writes it again.
struct WatchBalanceTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> WatchBalanceEntry {
        WatchBalanceEntry(date: .now, balances: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (WatchBalanceEntry) -> Void) {
        let balances = WidgetBalancesStore.read() ?? (context.isPreview ? .preview : .unknown)
        completion(WatchBalanceEntry(date: .now, balances: balances))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WatchBalanceEntry>) -> Void) {
        let entry = WatchBalanceEntry(date: .now, balances: WidgetBalancesStore.read() ?? .unknown)
        completion(Timeline(entries: [entry], policy: .never))
    }
}

#Preview("Circular", as: .accessoryCircular) {
    WatchBalanceWidget()
} timeline: {
    WatchBalanceEntry(date: .now, balances: .preview)
}

#Preview("Rectangular", as: .accessoryRectangular) {
    WatchBalanceWidget()
} timeline: {
    WatchBalanceEntry(date: .now, balances: .preview)
}
