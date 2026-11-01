import Observation
import Shared
import SwiftUI

@Observable
final class GroupStatsModel {
    private(set) var state: GroupStatsUiState

    @ObservationIgnored private let holder: ViewModelHolder<GroupStatsViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteGroupStats) {
        holder = BlitzSplitApple.shared.createGroupStatsViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupStatsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any GroupStatsUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Statistics" (6.6): what the group spent in a period and who spent it, from the bills on the device, offline too.
/// Pushed over the group with the system's Back and no "Split a bill". iPhone upright and iPad upright scroll one
/// column (the total, By person, Over time, Biggest expense); lying down, iPad and a Mac window put Over time and the
/// biggest bill in a second column. The Mac keeps the period in a toolbar pull-down and the total in four panels.
struct GroupStatsView: View {
    let route: RouteGroupStats

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: GroupStatsModel?
    @State private var isPickingDates = false
    @State private var layout = ScreenLayout()

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                StatsScroll(model: model, layout: layout, pickDates: { isPickingDates = true })
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .measuringScreenLayout($layout)
        .navigationTitle("Statistics")
        #if os(iOS)
        .toolbarTitleDisplayMode(.large)
        // On iPhone the statistics keep the group's own bars, without the app's tabs
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        #else
        .navigationSubtitle(macSubtitle)
        .toolbar {
            if let model, model.state.showsPeriodPicker {
                ToolbarItem(placement: .primaryAction) {
                    StatsPeriodMenu(model: model, pickDates: { isPickingDates = true })
                }
            }
        }
        #endif
        .sheet(isPresented: $isPickingDates) {
            if let model {
                StatsDatesSheet(state: model.state) { start, end in
                    isPickingDates = false
                    model.send(GroupStatsUiEventOnCustomPeriodPicked(startMillis: start, endMillis: end))
                }
            }
        }
        .onAppear {
            if model == nil { model = GroupStatsModel(route: route) }
        }
    }

    #if os(macOS)
    /// "Viagem Floripa · Oct 24 – 29" under the window's title.
    private var macSubtitle: String {
        guard let state = model?.state, state.isLoaded else { return "" }
        guard state.showsPeriodPicker else { return state.groupName }
        let range = GroupStatsCopy.range(state.periodRangeStyle, start: state.periodStartMillis, end: state.periodEndMillis)
        return "\(state.groupName) · \(range)"
    }
    #endif
}

/// The period's pull-down: each period with its dates and bills, "Pick dates…" after a divider.
private struct StatsPeriodMenu: View {
    let model: GroupStatsModel
    let pickDates: () -> Void

    var body: some View {
        let state = model.state
        Menu {
            ForEach(state.periodOptions, id: \.period) { option in
                if option.period == .custom { Divider() }
                Button {
                    if option.period == .custom {
                        pickDates()
                    } else {
                        model.send(GroupStatsUiEventOnPeriodPicked(period: option.period))
                    }
                } label: {
                    Label {
                        Text(verbatim: GroupStatsCopy.period(option.period, isTrip: state.isTrip))
                        Text(verbatim: GroupStatsCopy.hint(option))
                    } icon: {
                        if option.period == state.period { Image(systemName: "checkmark") }
                    }
                }
            }
        } label: {
            Label {
                Text(verbatim: GroupStatsCopy.period(state.period, isTrip: state.isTrip))
            } icon: {
                Image(systemName: "calendar")
            }
            .labelStyle(.titleAndIcon)
        }
        .accessibilityLabel(Text("Period"))
        .accessibilityValue(Text(verbatim: GroupStatsCopy.period(state.period, isTrip: state.isTrip)))
    }
}

private struct StatsScroll: View {
    let model: GroupStatsModel
    let layout: ScreenLayout
    let pickDates: () -> Void

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                #if os(iOS)
                StatsTopLine(model: model, pickDates: pickDates)
                #endif
                if state.showsStats {
                    if layout.usesTwoColumns {
                        #if os(macOS)
                        StatsSummaryPanels(state: state)
                            .padding(.bottom, 4)
                        #endif
                        HStack(alignment: .top, spacing: 16) {
                            VStack(alignment: .leading, spacing: 0) {
                                #if os(iOS)
                                StatsHeroCard(state: state)
                                #endif
                                StatsPeopleSection(model: model)
                            }
                            VStack(alignment: .leading, spacing: 0) {
                                StatsOverTimeSection(model: model)
                                StatsBiggestSection(model: model)
                            }
                        }
                    } else {
                        #if os(macOS)
                        StatsSummaryPanels(state: state)
                        #else
                        StatsHeroCard(state: state)
                        #endif
                        StatsPeopleSection(model: model)
                        StatsOverTimeSection(model: model)
                        StatsBiggestSection(model: model)
                    }
                } else {
                    StatsEmptyCard(model: model)
                        .padding(.top, 16)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
            .frame(maxWidth: layout.usesTwoColumns ? 1100 : 640)
            .frame(maxWidth: .infinity)
        }
        .background(Palette.groupedBackground)
    }
}

#if os(iOS)
/// The group's name under the large title, then the period's pull-down and its dates.
private struct StatsTopLine: View {
    let model: GroupStatsModel
    let pickDates: () -> Void

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 12) {
            Text(verbatim: state.groupName)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            if state.showsPeriodPicker {
                HStack(spacing: 10) {
                    StatsPeriodMenu(model: model, pickDates: pickDates)
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                        .fontWeight(.semibold)
                    Text(verbatim: GroupStatsCopy.range(
                        state.periodRangeStyle,
                        start: state.periodStartMillis,
                        end: state.periodEndMillis,
                    ))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 4)
        .padding(.top, 4)
        .padding(.bottom, 14)
    }
}

/// The period's total, its bills (the queue's with the orange dot) and the average, then this person's part.
private struct StatsHeroCard: View {
    let state: GroupStatsUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Total spent")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(verbatim: state.currency.format(state.totalMinor))
                .font(.largeTitle.bold())
                .monospacedDigit()
            HStack(spacing: 6) {
                if state.pendingCount > 0 {
                    Circle().fill(Color.orange).frame(width: 8, height: 8)
                }
                Text(verbatim: meta)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            if let share = state.myShareMinor?.int64Value {
                Divider().padding(.vertical, 10)
                HStack(alignment: .top, spacing: 12) {
                    figure(
                        title: String(localized: "Your share"),
                        value: state.currency.format(share),
                        hint: String(localized: "\(Int(state.mySharePercent))% of the total"),
                    )
                    figure(
                        title: String(localized: "You paid"),
                        value: state.currency.format(state.myPaidMinor?.int64Value ?? 0),
                        hint: GroupStatsCopy.expenseCount(state.myPaidCount),
                    )
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 26))
        .accessibilityElement(children: .combine)
    }

    /// "7 expenses, 1 waiting to be sent · average of $403.10 per person".
    private var meta: String {
        var count = GroupStatsCopy.expenseCount(state.expenseCount)
        if state.pendingCount > 0 {
            count += ", " + String(localized: "\(Int(state.pendingCount)) waiting to be sent")
        }
        return String(localized: "\(count) · average of \(state.currency.format(state.averagePerPersonMinor)) per person")
    }

    private func figure(title: String, value: String, hint: String) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(verbatim: title).font(.footnote).foregroundStyle(.secondary)
            Text(verbatim: value).font(.title3.weight(.semibold)).monospacedDigit()
            Text(verbatim: hint).font(.footnote).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
#endif

#if os(macOS)
/// The Mac's four panels: total, bills, average per person and this person's part.
private struct StatsSummaryPanels: View {
    let state: GroupStatsUiState

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 10) {
            panel(
                String(localized: "Total spent"),
                state.currency.format(state.totalMinor),
                GroupStatsCopy.period(state.period, isTrip: state.isTrip),
            )
            panel(
                String(localized: "Expenses"),
                "\(Int(state.expenseCount))",
                state.pendingCount > 0
                    ? String(localized: "\(Int(state.pendingCount)) waiting to be sent")
                    : String(localized: "settle-ups don't count"),
                isPending: state.pendingCount > 0,
            )
            panel(
                String(localized: "Average per person"),
                state.currency.format(state.averagePerPersonMinor),
                String(localized: "\(Int(state.memberCount)) people"),
            )
            if let share = state.myShareMinor?.int64Value {
                let paid = state.currency.format(state.myPaidMinor?.int64Value ?? 0)
                panel(
                    String(localized: "Your share"),
                    state.currency.format(share),
                    String(localized: "\(Int(state.mySharePercent))% of the total · you paid \(paid)"),
                )
            }
        }
        .padding(.bottom, 12)
    }

    private func panel(_ title: String, _ value: String, _ hint: String, isPending: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: title).font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
            Text(verbatim: value).font(.title2.bold()).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
            HStack(spacing: 5) {
                if isPending { Circle().fill(Color.orange).frame(width: 7, height: 7) }
                Text(verbatim: hint).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }
}
#endif

/// A section's title, upper case on iPhone and iPad as a grouped list's header, bold on the Mac.
struct StatsSectionTitle: View {
    let title: LocalizedStringKey

    var body: some View {
        #if os(macOS)
        Text(title)
            .font(.headline)
            .accessibilityAddTraits(.isHeader)
        #else
        Text(title)
            .font(.footnote)
            .textCase(.uppercase)
            .foregroundStyle(.secondary)
            .accessibilityAddTraits(.isHeader)
        #endif
    }
}

/// "By person": Consumed or Paid, a bar per person scaled by the largest, and what each one means.
private struct StatsPeopleSection: View {
    let model: GroupStatsModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 8) {
            StatsSectionTitle(title: "By person")
                .padding(.horizontal, 20)
                .padding(.top, 18)
            Picker("Show by person", selection: viewBinding) {
                Text("Consumed").tag(StatsPeopleView.consumption)
                Text("Paid").tag(StatsPeopleView.paid)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            VStack(spacing: 0) {
                ForEach(Array(state.people.enumerated()), id: \.element.memberId) { index, person in
                    StatsPersonLine(person: person, currency: state.currency)
                    if index < state.people.count - 1 {
                        Divider().padding(.leading, 64)
                    }
                }
            }
            .background(Palette.card, in: .rect(cornerRadius: 26))
            Group {
                if state.peopleView == .paid {
                    Text("What each one paid at the time. The difference between what they paid and what they consumed becomes a balance, in Balances.")
                } else {
                    Text("Consumed is each one's share of the expenses, as each bill was split.")
                }
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 20)
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var viewBinding: Binding<StatsPeopleView> {
        Binding(
            get: { model.state.peopleView },
            set: { model.send(GroupStatsUiEventOnPeopleViewPicked(view: $0)) },
        )
    }
}

private struct StatsPersonLine: View {
    let person: StatsPersonRow
    let currency: GroupCurrency

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 36)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text(verbatim: GroupStatsCopy.name(person))
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Text(verbatim: currency.format(person.amountMinor))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(person.amountMinor > 0 ? .primary : .secondary)
                }
                StatsAmountBar(fraction: person.barFraction)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: "\(GroupStatsCopy.name(person)): \(currency.format(person.amountMinor))"))
    }
}

private struct StatsAmountBar: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(.fill.tertiary)
                Capsule().fill(Palette.accent).frame(width: geometry.size.width * fraction)
            }
        }
        .frame(height: 6)
    }
}

/// "Over time": Bars or Line, the grain, the chart, the most expensive one and why some grains are off.
private struct StatsOverTimeSection: View {
    let model: GroupStatsModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                StatsSectionTitle(title: "Over time")
                Spacer()
                Picker("Chart type", selection: chartBinding) {
                    Label("Bars", systemImage: "chart.bar").labelStyle(.iconOnly).tag(StatsChartType.bars)
                    Label("Line", systemImage: "chart.xyaxis.line").labelStyle(.iconOnly).tag(StatsChartType.line)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .fixedSize()
            }
            .padding(.leading, 20)
            .padding(.top, 14)
            VStack(alignment: .leading, spacing: 10) {
                StatsGrainSegments(model: model)
                StatsChart(state: state)
                if let peak = state.peak {
                    HStack(spacing: 4) {
                        Text(verbatim: GroupStatsCopy.peak(peak, grain: state.grain))
                            .foregroundStyle(.secondary)
                        Text(verbatim: state.currency.format(peak.amountMinor))
                            .fontWeight(.semibold)
                            .monospacedDigit()
                    }
                    .font(.footnote)
                }
            }
            .padding(16)
            .background(Palette.card, in: .rect(cornerRadius: 26))
            if let note = GroupStatsCopy.grainNote(state) {
                Text(verbatim: note)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var chartBinding: Binding<StatsChartType> {
        Binding(
            get: { model.state.chartType },
            set: { model.send(GroupStatsUiEventOnChartTypePicked(chartType: $0)) },
        )
    }
}

/// Day, Week, Month and Year as a segmented control whose segments the period can't show are dimmed and do nothing.
private struct StatsGrainSegments: View {
    let model: GroupStatsModel

    var body: some View {
        let state = model.state
        HStack(spacing: 0) {
            ForEach(state.grains, id: \.grain) { option in
                let isSelected = option.grain == state.grain
                Button {
                    model.send(GroupStatsUiEventOnGrainPicked(grain: option.grain))
                } label: {
                    Text(verbatim: GroupStatsCopy.grain(option.grain))
                        .font(.footnote.weight(isSelected ? .semibold : .regular))
                        .frame(maxWidth: .infinity, minHeight: 28)
                        .background {
                            if isSelected {
                                Capsule()
                                    .fill(Palette.segmentThumb)
                                    .shadow(color: .black.opacity(0.1), radius: 3, y: 1)
                            }
                        }
                        .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .disabled(!option.isEnabled)
                .opacity(option.isEnabled ? 1 : 0.38)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(2)
        .background(.fill.tertiary, in: .capsule)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("Group by"))
    }
}

/// "Biggest expense", which opens the bill.
private struct StatsBiggestSection: View {
    let model: GroupStatsModel

    var body: some View {
        if let expense = model.state.biggestExpense {
            VStack(alignment: .leading, spacing: 8) {
                StatsSectionTitle(title: "Biggest expense")
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                Button {
                    model.send(GroupStatsUiEventOnBiggestExpenseClicked.shared)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "receipt")
                            .foregroundStyle(Palette.accent)
                            .frame(width: 36, height: 36)
                            .background(Palette.groupedBackground, in: .rect(cornerRadius: 10))
                        VStack(alignment: .leading, spacing: 1) {
                            Text(verbatim: expense.description_)
                                .lineLimit(1)
                            HStack(spacing: 5) {
                                if expense.isPending {
                                    Circle().fill(Color.orange).frame(width: 7, height: 7)
                                }
                                Text(verbatim: GroupStatsCopy.biggestMeta(expense))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer(minLength: 8)
                        Text(verbatim: model.state.currency.format(expense.amountMinor))
                            .font(.subheadline.weight(.semibold))
                            .monospacedDigit()
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                    .padding(.leading, 16)
                    .padding(.trailing, 14)
                    .padding(.vertical, 10)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .background(Palette.card, in: .rect(cornerRadius: 26))
            }
        }
    }
}

/// "Nothing to show yet" with "Split a bill", or a period without bills with "See the whole trip".
private struct StatsEmptyCard: View {
    let model: GroupStatsModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            Image(systemName: "chart.bar")
                .font(.title)
                .foregroundStyle(Palette.accent)
                .frame(width: 64, height: 64)
                .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 18))
            if let start = state.allStartMillis?.int64Value, let end = state.allEndMillis?.int64Value, state.isPeriodEmpty {
                Text(verbatim: GroupStatsCopy.periodEmptyTitle(state))
                    .font(.title3.weight(.semibold))
                Text("All of this group's expenses are from \(GroupStatsCopy.range(state.allRangeStyle, start: start, end: end)).")
                    .foregroundStyle(.secondary)
                Button {
                    model.send(GroupStatsUiEventOnWholePeriodClicked.shared)
                } label: {
                    Group {
                        if state.isTrip {
                            Text("See the whole trip")
                        } else {
                            Text("See all time")
                        }
                    }
                    .fontWeight(.semibold)
                        .frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .controlSize(.large)
            } else {
                Text("Nothing to show yet")
                    .font(.title3.weight(.semibold))
                Text("The statistics show up once the first expense is added.")
                    .foregroundStyle(.secondary)
                Button {
                    model.send(GroupStatsUiEventOnSplitBillClicked.shared)
                } label: {
                    Text("Split a bill")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity, minHeight: 36)
                }
                #if os(iOS)
                .buttonStyle(.glassProminent)
                #else
                .buttonStyle(.borderedProminent)
                #endif
                .buttonBorderShape(.capsule)
                .controlSize(.large)
            }
        }
        .multilineTextAlignment(.center)
        .padding(.horizontal, 24)
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }
}

/// "Pick dates": the first and the last day, never after today.
private struct StatsDatesSheet: View {
    let onPick: (Int64, Int64) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var start: Date
    @State private var end: Date

    init(state: GroupStatsUiState, onPick: @escaping (Int64, Int64) -> Void) {
        self.onPick = onPick
        _start = State(initialValue: ExpenseFormat.date(of: state.periodStartMillis))
        _end = State(initialValue: ExpenseFormat.date(of: state.periodEndMillis))
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
        #else
        .frame(width: 360, height: 220)
        #endif
    }
}
