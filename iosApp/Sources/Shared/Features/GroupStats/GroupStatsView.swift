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

    /// A category of "By category", which opens its bills of the period.
    func openCategory(_ categoryId: String, _ target: StatsCategoryTarget) {
        send(GroupStatsUiEventOnCategoryClicked(categoryId: categoryId, target: target))
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Statistics" (6.6): what the group spent in a period and who spent it, from the bills on the device, offline too.
/// Pushed over the group with the system's Back and no "Split a bill"; an iPad lying down pushes it in the group's
/// column beside the groups (decision 2). iPhone upright and iPad upright scroll one column (the total, By category,
/// By person, Settling up, Over time, Biggest expense), split on the fold of an iPhone Duo open upright; lying down,
/// in the iPad's column and across an iPhone Duo open lying down, the total and By category take the first column and
/// By person, Settling up, Over time and the biggest bill the second, as the boards draw them. A Mac window keeps By
/// person and Settling up under By category, its period in a toolbar pull-down and the total in four panels.
struct GroupStatsView: View {
    let route: RouteGroupStats

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: GroupStatsModel?
    @State private var isPickingDates = false
    @State private var layout = ScreenLayout()

    /// In the iPad's column beside the groups, upright as it is, the boards still draw two columns.
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                StatsScroll(
                    model: model,
                    usesTwoColumns: layout.usesTwoColumns || showsGroupBesideList,
                    pickDates: { openDatesPicker(model) },
                )
            } else {
                // The bills not read yet: the totals, the ring and the people shimmer in place
                ShimmerScreen {
                    StatsPlaceholder()
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
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
                    StatsPeriodMenu(model: model, pickDates: { openDatesPicker(model) })
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
                .foldSheetPlacement()
            }
        }
        .onAppear {
            if model == nil { model = GroupStatsModel(route: route) }
        }
    }

    /// "Pick dates…": the tap goes to the shared code, the picker is this view's own sheet.
    private func openDatesPicker(_ model: GroupStatsModel) {
        model.send(GroupStatsUiEventOnPickDatesClicked.shared)
        isPickingDates = true
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
    let usesTwoColumns: Bool
    let pickDates: () -> Void

    var body: some View {
        #if os(iOS)
        if !usesTwoColumns {
            // An iPhone Duo open upright: the total and Over time above the fold, the categories, By person,
            // Settling up and the biggest bill below it (6.6)
            FoldHalves(splits: model.state.showsStats) {
                VStack(alignment: .leading, spacing: 0) {
                    StatsTopLine(model: model, pickDates: pickDates)
                    StatsCurrencyChips(model: model)
                    StatsHeroCard(state: model.state)
                    StatsOverTimeSection(model: model)
                }
                .padding(.horizontal, 16)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            } bottom: {
                VStack(alignment: .leading, spacing: 0) {
                    StatsCategoriesSection(state: model.state, open: model.openCategory)
                    StatsPeopleSection(model: model)
                    StatsSettlingSection(model: model)
                    StatsBiggestSection(model: model)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            } single: {
                scroll
            }
            .background(Palette.groupedBackground)
        } else {
            scroll
        }
        #else
        scroll
        #endif
    }

    private var scroll: some View {
        let state = model.state
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                #if os(iOS)
                StatsTopLine(model: model, pickDates: pickDates)
                #endif
                StatsCurrencyChips(model: model)
                if state.showsStats {
                    if usesTwoColumns {
                        #if os(macOS)
                        StatsSummaryPanels(state: state)
                            .padding(.bottom, 4)
                        #endif
                        // An iPhone Duo open splits on its fold (the boards' 6.6)
                        TwoColumnsLayout(spacing: 16, maxWidth: 1100) {
                            VStack(alignment: .leading, spacing: 0) {
                                #if os(iOS)
                                StatsHeroCard(state: state)
                                #endif
                                StatsCategoriesSection(state: state, pieSize: 112, open: model.openCategory)
                                #if os(macOS)
                                StatsPeopleSection(model: model)
                                StatsSettlingSection(model: model)
                                #endif
                            }
                        } second: {
                            VStack(alignment: .leading, spacing: 0) {
                                #if os(iOS)
                                StatsPeopleSection(model: model)
                                StatsSettlingSection(model: model)
                                #endif
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
                        StatsCategoriesSection(state: state, open: model.openCategory)
                        StatsPeopleSection(model: model)
                        StatsSettlingSection(model: model)
                        StatsOverTimeSection(model: model)
                        StatsBiggestSection(model: model)
                    }
                } else if state.isLoadingBills {
                    // A first download may still bring this period's bills: never "Nothing to show yet" before them
                    VStack(alignment: .leading, spacing: 16) {
                        StatsPlaceholder()
                            .shimmering()
                        if state.isWaitingForInternet {
                            WaitingForInternetNote()
                        }
                    }
                    .padding(.top, 16)
                } else {
                    StatsEmptyCard(model: model)
                        .padding(.top, 16)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
            .frame(maxWidth: usesTwoColumns ? 1100 : 640)
            .frame(maxWidth: .infinity)
        }
        .background(Palette.groupedBackground)
    }
}

/// The currency picked, above every figure of the screen, while the group has money in more than one.
private struct StatsCurrencyChips: View {
    let model: GroupStatsModel

    var body: some View {
        if model.state.currencies.count > 1 {
            CurrencyChips(
                currencies: model.state.currencies,
                selected: model.state.currency,
            ) { model.send(GroupStatsUiEventOnCurrencySelected(currency: $0)) }
                .padding(.vertical, 8)
        }
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

/// The period's total, its bills (the queue's with the amber dot) and the average, then this person's part.
private struct StatsHeroCard: View {
    let state: GroupStatsUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: GroupStatsCopy.totalLabel(state))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(verbatim: state.currency.format(state.totalMinor))
                .font(.largeTitle.bold())
                .monospacedDigit()
            HStack(spacing: 6) {
                if state.pendingCount > 0 {
                    PendingDot()
                }
                Text(verbatim: meta)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            if let share = state.myShareMinor?.int64Value {
                Divider().padding(.vertical, 10)
                HStack(alignment: .top, spacing: 12) {
                    figure(
                        title: String(localized: "Your share", bundle: .appLanguage),
                        value: state.currency.format(share),
                        hint: String(localized: "\(Int(state.mySharePercent))% of the total", bundle: .appLanguage),
                    )
                    figure(
                        title: String(localized: "You paid", bundle: .appLanguage),
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
            count += ", " + String(localized: "\(Int(state.pendingCount)) waiting to be sent", bundle: .appLanguage)
        }
        return String(
            localized: "\(count) · average of \(state.currency.format(state.averagePerPersonMinor)) per person",
            bundle: .appLanguage,
        )
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
/// The Mac's four panels: total, bills, average per person and this person's part; all in a row while each keeps
/// 170 pt, two by two in a narrower window, so "38% of the total · you paid R$ 1.234,56" still reads whole.
private struct StatsSummaryPanels: View {
    let state: GroupStatsUiState

    /// The grid's width, wide until measured, the window's usual room
    @State private var width = CGFloat.greatestFiniteMagnitude

    private var columnCount: Int {
        let panels = state.myShareMinor == nil ? 3 : 4
        return width >= CGFloat(panels) * 170 + CGFloat(panels - 1) * 10 ? panels : 2
    }

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: columnCount), spacing: 10) {
            panel(
                GroupStatsCopy.totalLabel(state),
                state.currency.format(state.totalMinor),
                GroupStatsCopy.period(state.period, isTrip: state.isTrip),
            )
            panel(
                String(localized: "Expenses", bundle: .appLanguage),
                "\(Int(state.expenseCount))",
                state.pendingCount > 0
                    ? String(localized: "\(Int(state.pendingCount)) waiting to be sent", bundle: .appLanguage)
                    : String(localized: "settle-ups don't count", bundle: .appLanguage),
                isPending: state.pendingCount > 0,
            )
            panel(
                String(localized: "Average per person", bundle: .appLanguage),
                state.currency.format(state.averagePerPersonMinor),
                String(localized: "\(Int(state.memberCount)) people", bundle: .appLanguage),
            )
            if let share = state.myShareMinor?.int64Value {
                let paid = state.currency.format(state.myPaidMinor?.int64Value ?? 0)
                panel(
                    String(localized: "Your share", bundle: .appLanguage),
                    state.currency.format(share),
                    String(
                        localized: "\(Int(state.mySharePercent))% of the total · you paid \(paid)",
                        bundle: .appLanguage,
                    ),
                )
            }
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }) { width = $0 }
        .padding(.bottom, 12)
    }

    private func panel(_ title: String, _ value: String, _ hint: String, isPending: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: title).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
            Text(verbatim: value).font(.title2.bold()).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
            HStack(spacing: 5) {
                if isPending { PendingDot(size: 7) }
                // Two lines kept for every panel, so a row's panels stay one height
                Text(verbatim: hint).font(.caption).foregroundStyle(.secondary).lineLimit(2, reservesSpace: true)
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
                    StatsPersonLine(person: person, currency: state.currency) {
                        model.send(GroupStatsUiEventOnPersonClicked(memberId: person.memberId))
                    }
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

/// A person of "By person": the row opens their profile (Profile for you), which a former member doesn't have.
private struct StatsPersonLine: View {
    let person: StatsPersonRow
    let currency: GroupCurrency
    let onOpen: () -> Void

    var body: some View {
        StatsAvatarRow(
            initial: person.initials,
            color: person.avatarColor,
            photoUrl: person.photoUrl,
            label: "\(GroupStatsCopy.name(person)): \(currency.format(person.amountMinor))",
        ) {
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
                StatsAmountBar(fraction: person.barFraction, color: person.avatarColor.data)
            }
        }
        .opensProfile(person.opensProfile ? onOpen : nil)
    }
}

/// A person's row of By person and Settling up, read by VoiceOver as one sentence.
struct StatsAvatarRow<Content: View>: View {
    let initial: String
    let color: AvatarColor
    let photoUrl: String?
    let label: String
    @ViewBuilder let content: Content

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: initial, color: color, photoUrl: photoUrl, size: 36)
                .accessibilityHidden(true)
            content
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: label))
    }
}

/// The bar takes the color of what it measures (the category's slice, the person's face), never the accent.
struct StatsAmountBar: View {
    let fraction: Double
    let color: Color

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(.fill.tertiary)
                Capsule().fill(color).frame(width: geometry.size.width * fraction)
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
                if let average = state.chartAverageMinor {
                    StatsAverageLegend(
                        label: GroupStatsCopy.average(grain: state.grain),
                        amount: state.currency.format(Int64(truncating: average)),
                    )
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
                        CategoryBadge(category: expense.category, size: 36)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(verbatim: expense.description_)
                                .lineLimit(1)
                            HStack(spacing: 5) {
                                if expense.isPending {
                                    PendingDot(size: 7)
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

/// The statistics still coming: the total, the ring by category and the people, as blocks the caller makes shimmer.
private struct StatsPlaceholder: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ShimmerCard(showsButtons: false)
                .background(Palette.card, in: .rect(cornerRadius: 26))
            HStack(spacing: 16) {
                ShimmerCircle(size: 112)
                VStack(alignment: .leading, spacing: 10) {
                    ShimmerLine(height: 12)
                    ShimmerLine(width: 140, height: 12)
                    ShimmerLine(width: 100, height: 12)
                }
            }
            ShimmerList(count: 3)
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
                .prominentLabel()
                #else
                .buttonStyle(.borderedProminent)
                .prominentLabel()
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
        // An iPhone Duo open: in the half before the fold, never over it
        .foldSheetPlacement()
        #else
        .frame(width: 360, height: 220)
        #endif
    }
}

/// The dashed average line's key before "Average per day: $405"; tapping it (or the pointer over it on the Mac) says
/// what the dashed line is.
private struct StatsAverageLegend: View {
    let label: String
    let amount: String
    @State private var isHintShown = false
    // A click keeps the hint open after the pointer leaves, until it is dismissed
    @State private var isHintPinned = false
    private let hint = String(
        localized: "The darker dashed line across the chart, like the one in this legend, is the average: the total spent divided by every period shown, the empty ones included.",
        bundle: .appLanguage,
    )

    var body: some View {
        Button {
            // On the Mac the click dismisses the hover's popover first, so this pins it again
            isHintPinned = !isHintShown
            isHintShown = !isHintShown
        } label: {
            HStack(spacing: 4) {
                // The dashed line's key, as the chart draws it
                Path { path in
                    path.move(to: CGPoint(x: 0, y: 1))
                    path.addLine(to: CGPoint(x: 16, y: 1))
                }
                .stroke(Color.secondary, style: StrokeStyle(lineWidth: 2, dash: [4, 3]))
                .frame(width: 16, height: 2)
                .padding(.trailing, 2)
                Text(verbatim: label)
                    .foregroundStyle(.secondary)
                Text(verbatim: amount)
                    .fontWeight(.semibold)
                    .monospacedDigit()
            }
            .font(.footnote)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint(Text(verbatim: hint))
        .popover(isPresented: $isHintShown, arrowEdge: .bottom) {
            Text(verbatim: hint)
                .font(.footnote)
                .padding(12)
                .frame(width: 240, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .presentationCompactAdaptation(.popover)
        }
        .onChange(of: isHintShown) { _, isShown in
            if !isShown { isHintPinned = false }
        }
        #if os(macOS)
        .onHover { isHovering in
            if isHovering {
                isHintShown = true
            } else if !isHintPinned {
                isHintShown = false
            }
        }
        #endif
    }
}
