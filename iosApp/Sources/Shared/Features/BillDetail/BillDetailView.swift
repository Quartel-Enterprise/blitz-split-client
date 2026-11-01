import Observation
import Shared
import SwiftUI

@Observable
final class BillDetailModel {
    private(set) var state: BillDetailUiState
    private(set) var deleteErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<BillDetailViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteBillDetail) {
        holder = BlitzSplitApple.shared.createBillDetailViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! BillDetailUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is BillDetailUiActionShowDeleteError { self?.deleteErrorCount += 1 }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: "Delete" has
    /// already started deleting by the time the alert reports itself closed.
    var current: BillDetailUiState {
        (holder.viewModel.uiState.value as? BillDetailUiState) ?? state
    }

    func send(_ event: any BillDetailUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The alert closed without "Delete"; the ViewModel keeps it while the bill is being deleted.
    func dismissDeletion() {
        if current.deletion != nil { send(BillDetailUiEventOnDeleteDismissed.shared) }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Bill": one bill of the group, what it was, who paid and how it splits. Pushed over the group on iPhone, a form
/// sheet on iPad and a sheet on the Mac's window; Edit and Delete in the toolbar (on the Mac, in the sheet's header).
struct BillDetailView: View {
    let route: RouteBillDetail

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: BillDetailModel?

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    /// The window under the sheet, not the sheet: whether this bill is a sheet that closes or a pushed screen.
    @Environment(\.windowLayout) private var windowLayout
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacBillDetail(model: model)
                #else
                PhoneBillDetail(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
                    #if os(macOS)
                    .frame(width: 560, height: 480)
                    #endif
            }
        }
        #if os(iOS)
        .navigationTitle(Text(verbatim: model?.state.groupName ?? ""))
        .navigationBarTitleDisplayMode(.inline)
        // On iPhone the bill keeps the group's own bars, without the app's tabs
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        .toolbar { toolbar }
        #endif
        .billDeletionAlert(
            model?.state.deletion,
            currency: model?.state.currency,
            isDeleting: model?.state.isDeleting == true,
            onConfirm: { model?.send(BillDetailUiEventOnDeleteConfirmed.shared) },
            onDismiss: { model?.dismissDeletion() },
        )
        .glassToast("Couldn't delete the bill. Try again.", trigger: model?.deleteErrorCount ?? 0)
        .onAppear {
            if model == nil { model = BillDetailModel(route: route) }
        }
    }

    #if os(iOS)
    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        // A form sheet on iPad has no back button: it closes
        if AppRoute.billDetail(route).presentsAsSheet(isLargeWindow: windowLayout.isLarge) {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(BillDetailUiEventOnBackClicked.shared)
                }
            }
        }
        ToolbarItem(placement: .primaryAction) {
            Button("Edit") {
                model?.send(BillDetailUiEventOnEditClicked.shared)
            }
            .disabled(model?.state.areActionsEnabled != true)
        }
        ToolbarItem(placement: .primaryAction) {
            Button(role: .destructive) {
                model?.send(BillDetailUiEventOnDeleteClicked.shared)
            } label: {
                Label {
                    Text(BillCopy.delete)
                } icon: {
                    Image(systemName: "trash")
                }
            }
            .disabled(model?.state.areActionsEnabled != true)
        }
    }
    #endif
}

/// "Bruno paid" next to Bruno's face, or "A former member paid".
private struct PayerLine: View {
    let payers: [BillPayer]
    let size: CGFloat

    var body: some View {
        HStack(spacing: 8) {
            // Whoever left the group has no face
            let faces = payers.filter { $0.name != nil }.prefix(3)
            if !faces.isEmpty {
                HStack(spacing: -size * 0.3) {
                    ForEach(Array(faces.enumerated()), id: \.offset) { _, payer in
                        AvatarPhoto(initial: payer.initials, color: payer.avatarColor, photoUrl: nil, size: size)
                    }
                }
                .accessibilityHidden(true)
            }
            Text(verbatim: BillCopy.paid(payers))
        }
    }
}

/// "Queued · syncs when you're back online", with the orange dot of what is still queued.
private struct PendingLine: View {
    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(.orange)
                .frame(width: 8, height: 8)
                .accessibilityHidden(true)
            Text("Queued · syncs when you're back online")
        }
    }
}

/// "−R$ 118,00 you owe" in the negative color, "+R$ 249,60 to get back" in the accent, and who settles it.
private struct MyPartContent: View {
    let state: BillDetailUiState

    var body: some View {
        if let minor = state.myBalanceMinor?.int64Value {
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(verbatim: BillCopy.myPartAmount(minor, currency: state.currency))
                        .font(.title2.bold())
                        .monospacedDigit()
                    Text(verbatim: BillCopy.myPartWord(minor))
                        .font(.body.weight(.semibold))
                }
                .foregroundStyle(Palette.balance(minor))
                if let note = BillCopy.myPartNote(state.myPartNote, currency: state.currency) {
                    Text(verbatim: note)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
        }
    }
}

/// One person of the split: their face, "Paid R$ 642,00" or "Hasn't joined yet", and their part.
private struct ShareRow: View {
    let row: BillShareRow
    let currency: GroupCurrency
    let avatarSize: CGFloat

    var body: some View {
        HStack(spacing: 12) {
            ShareAvatar(row: row, size: avatarSize)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: BillCopy.name(row))
                    .lineLimit(1)
                if let subtitle = BillCopy.subtitle(row, currency: currency) {
                    Text(verbatim: subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            Text(verbatim: BillCopy.share(row, currency: currency))
                .monospacedDigit()
                .foregroundStyle(row.shareMinor == nil ? .secondary : .primary)
        }
        .accessibilityElement(children: .combine)
    }
}

/// Someone who left the group has no name nor color left: a plain person.
private struct ShareAvatar: View {
    let row: BillShareRow
    let size: CGFloat

    var body: some View {
        Group {
            if row.name == nil, !row.isMe {
                Image(systemName: "person.fill")
                    .font(.system(size: size * 0.45))
                    .foregroundStyle(.secondary)
                    .frame(width: size, height: size)
                    .background(.fill.tertiary, in: .circle)
            } else {
                AvatarPhoto(initial: row.initials, color: row.avatarColor, photoUrl: nil, size: size)
            }
        }
        .accessibilityHidden(true)
    }
}

#if os(iOS)
/// Upright and on a narrow iPad sheet: one column. Lying down and on a wide iPad sheet: the bill on the left, how it
/// splits on the right.
private struct PhoneBillDetail: View {
    let model: BillDetailModel

    @State private var layout = ScreenLayout()

    private var state: BillDetailUiState {
        model.state
    }

    var body: some View {
        Group {
            if layout.usesTwoColumns {
                HStack(alignment: .top, spacing: 0) {
                    list { billSections }
                    list { splitSection }
                }
            } else {
                list {
                    billSections
                    splitSection
                }
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
        }
        .measuringScreenLayout($layout)
        .background(Palette.groupedBackground)
    }

    private func list<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        List(content: content)
            .listStyle(.insetGrouped)
            .listSectionSpacing(.compact)
            .scrollContentBackground(.hidden)
    }

    @ViewBuilder
    private var billSections: some View {
        Section {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 12) {
                    ExpenseIcon(size: 52)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(verbatim: state.description_)
                            .font(.title.bold())
                            .lineLimit(3)
                            .accessibilityAddTraits(.isHeader)
                        Text(verbatim: ExpenseFormat.dayTime(state.spentAt, relativeDay: state.spentDay))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(verbatim: state.currency.format(state.totalMinor))
                        .font(.system(size: 40, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    PayerLine(payers: state.payers, size: 24)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if state.isPending {
                        PendingLine()
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 8, leading: 20, bottom: 4, trailing: 20))
        if state.showsMyPart {
            Section {
                MyPartContent(state: state)
                    .padding(.vertical, 4)
            } header: {
                Text("Your share")
            }
        }
    }

    private var splitSection: some View {
        Section {
            ForEach(state.people, id: \.memberId) { row in
                ShareRow(row: row, currency: state.currency, avatarSize: 32)
            }
        } header: {
            Text(verbatim: BillCopy.splitHeader(state.splitMode, count: state.participantCount))
        }
    }
}
#endif

#if os(macOS)
/// The sheet on the window: the bill's header with Edit… and Delete, the bill and its split, and Close.
private struct MacBillDetail: View {
    let model: BillDetailModel

    private var state: BillDetailUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(20)
            Divider()
            ScrollView {
                content
                    .padding(20)
            }
            .frame(minHeight: 240, idealHeight: 460, maxHeight: 620)
            Divider()
            HStack {
                Spacer()
                Button("Close") {
                    model.send(BillDetailUiEventOnBackClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
            }
            .controlSize(.large)
            .padding(16)
        }
        .frame(width: 560)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            ExpenseIcon(size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: state.description_)
                    .font(.title2.bold())
                    .lineLimit(2)
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: "\(state.groupName) · \(ExpenseFormat.dayTime(state.spentAt, relativeDay: state.spentDay))")
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 12)
            Button("Edit…") {
                model.send(BillDetailUiEventOnEditClicked.shared)
            }
            .disabled(!state.areActionsEnabled)
            Button(role: .destructive) {
                model.send(BillDetailUiEventOnDeleteClicked.shared)
            } label: {
                Label {
                    Text(BillCopy.delete)
                } icon: {
                    Image(systemName: "trash")
                }
                .labelStyle(.iconOnly)
            }
            .help(Text(BillCopy.delete))
            .disabled(!state.areActionsEnabled)
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: state.currency.format(state.totalMinor))
                    .font(.system(size: 34, weight: .bold))
                    .monospacedDigit()
                PayerLine(payers: state.payers, size: 20)
                    .foregroundStyle(.secondary)
                if state.isPending {
                    PendingLine()
                        .foregroundStyle(.secondary)
                }
            }
            if state.showsMyPart {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Your share")
                        .font(.headline)
                    MyPartContent(state: state)
                        .padding(12)
                        .background(Palette.card, in: .rect(cornerRadius: 10))
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(verbatim: BillCopy.splitHeader(state.splitMode, count: state.participantCount))
                    .font(.headline)
                VStack(spacing: 0) {
                    ForEach(Array(state.people.enumerated()), id: \.element.memberId) { index, row in
                        if index > 0 { Divider() }
                        ShareRow(row: row, currency: state.currency, avatarSize: 24)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 10))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
#endif
